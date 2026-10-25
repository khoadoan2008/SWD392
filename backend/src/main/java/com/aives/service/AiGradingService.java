package com.aives.service;

import com.aives.model.entity.InterviewSession;
import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.Rubric;
import com.aives.model.entity.SessionRubricScore;
import com.aives.repository.InterviewSessionRepository;
import com.aives.repository.InterviewTurnRepository;
import com.aives.repository.RubricRepository;
import com.aives.repository.SessionRubricScoreRepository;
import com.aives.service.generation.AiConfiguration;
import com.aives.service.generation.AiTransport;
import com.aives.service.generation.GeminiHttpTransport;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class AiGradingService {

    /** Nguồn tạo điểm gợi ý, lưu ở cột ai_grading_source để giảng viên biết điểm đến từ đâu. */
    public static final String SOURCE_GEMINI = "GEMINI";
    public static final String SOURCE_RULE_BASED = "RULE_BASED";
    public static final String SOURCE_NO_RUBRIC = "NO_RUBRIC";

    /** Điểm tổng (AI đề xuất và điểm chốt) đều quy về thang 10. */
    private static final BigDecimal SCALE_MAX = BigDecimal.TEN;

    private final AiConfiguration aiConfiguration;
    private final AiTransport transport;
    private final RubricRepository rubricRepository;
    private final SessionRubricScoreRepository sessionRubricScoreRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewTurnRepository turnRepository;

    @Autowired
    public AiGradingService(
            AiConfiguration aiConfiguration,
            RubricRepository rubricRepository,
            SessionRubricScoreRepository sessionRubricScoreRepository,
            InterviewSessionRepository sessionRepository,
            InterviewTurnRepository turnRepository) {
        this(aiConfiguration,
                new GeminiHttpTransport(aiConfiguration != null ? aiConfiguration.getModel() : "gemini-3.8-flash"),
                rubricRepository,
                sessionRubricScoreRepository,
                sessionRepository,
                turnRepository);
    }

    public AiGradingService(
            AiConfiguration aiConfiguration,
            AiTransport transport,
            RubricRepository rubricRepository,
            SessionRubricScoreRepository sessionRubricScoreRepository,
            InterviewSessionRepository sessionRepository,
            InterviewTurnRepository turnRepository) {
        this.aiConfiguration = aiConfiguration;
        this.transport = transport;
        this.rubricRepository = rubricRepository;
        this.sessionRubricScoreRepository = sessionRubricScoreRepository;
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
    }

    /** Dùng cho chấm nền sau khi buổi thi kết thúc: nạp lại buổi thi trong transaction mới. */
    @Transactional
    public void evaluateSessionById(Integer sessionId) {
        sessionRepository.findById(sessionId).ifPresent(this::evaluateSession);
    }

    @Transactional
    public InterviewSession evaluateSession(InterviewSession session) {
        if (session.getSubject() == null) {
            return session;
        }
        // Điểm đã được giảng viên khoá thì AI không được ghi đè.
        if (session.isGraded()) {
            return session;
        }

        List<Rubric> rubrics = rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(session.getSubject().getId());
        if (rubrics.isEmpty()) {
            session.setAiSuggestedScore(BigDecimal.ZERO);
            session.setAiFeedback("Môn học chưa được thiết lập tiêu chí Rubric để AI thực hiện chấm điểm.");
            session.setAiGradingSource(SOURCE_NO_RUBRIC);
            return sessionRepository.save(session);
        }

        List<InterviewTurn> turns = turnRepository.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(session.getId());

        GradingResult result = null;
        String source = SOURCE_RULE_BASED;
        String geminiError = null;
        if (aiConfiguration != null && aiConfiguration.isConfigured()) {
            try {
                result = evaluateWithGemini(session, rubrics, turns);
                source = SOURCE_GEMINI;
            } catch (Exception e) {
                geminiError = e.getMessage();
                log.warn("Gemini grading failed for session {}: {}. Falling back to rule-based evaluation.",
                        session.getId(), e.getMessage());
            }
        } else {
            geminiError = "Chưa cấu hình GEMINI_API_KEY.";
        }

        if (result == null) {
            result = evaluateWithRuleBased(rubrics, turns, geminiError);
        }

        // Clean previous rubric scores
        sessionRubricScoreRepository.deleteBySessionId(session.getId());
        sessionRubricScoreRepository.flush();

        List<SessionRubricScore> savedScores = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;

        for (Rubric r : rubrics) {
            RubricItemEvaluation item = result.evaluations.get(r.getId());
            BigDecimal score = BigDecimal.ZERO;
            String comment;

            if (item != null) {
                score = item.score.min(r.getMaxScore()).max(BigDecimal.ZERO);
                comment = item.comment;
            } else {
                comment = "Không tìm thấy dữ liệu đánh giá phù hợp trong biên bản trả lời.";
            }

            score = score.setScale(2, RoundingMode.HALF_UP);
            totalScore = totalScore.add(score);
            totalMax = totalMax.add(r.getMaxScore());

            SessionRubricScore srs = SessionRubricScore.builder()
                    .session(session)
                    .rubric(r)
                    .aiScore(score)
                    .finalScore(score) // Default lecturer draft to AI score for easy review
                    .comment(comment)
                    .build();
            savedScores.add(sessionRubricScoreRepository.save(srs));
        }

        session.setAiSuggestedScore(toTenPointScale(totalScore, totalMax));
        session.setAiFeedback(result.overallFeedback);
        session.setAiGradingSource(source);
        if (session.getRubricScores() != null) {
            session.getRubricScores().clear();
            session.getRubricScores().addAll(savedScores);
        } else {
            session.setRubricScores(new ArrayList<>(savedScores));
        }

        return sessionRepository.save(session);
    }

    /** Tổng điểm các tiêu chí quy về thang 10 theo tổng điểm tối đa của rubric môn học. */
    static BigDecimal toTenPointScale(BigDecimal total, BigDecimal totalMax) {
        if (totalMax == null || totalMax.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.multiply(SCALE_MAX).divide(totalMax, 2, RoundingMode.HALF_UP).min(SCALE_MAX.setScale(2));
    }

    private GradingResult evaluateWithGemini(InterviewSession session, List<Rubric> rubrics, List<InterviewTurn> turns) throws Exception {
        StringBuilder transcriptBuilder = new StringBuilder();
        for (InterviewTurn t : turns) {
            String typeLabel = t.isFollowUp() ? "HỎI XOÁY #" + t.getFollowupIndex() : "CÂU CHÍNH #" + t.getMainIndex();
            transcriptBuilder.append("[").append(typeLabel).append("] Câu hỏi: ").append(t.getQuestionText()).append("\n");
            transcriptBuilder.append("-> Trả lời: ").append(t.getTranscript() != null && !t.getTranscript().trim().isEmpty()
                    ? t.getTranscript().trim()
                    : "(Thí sinh không trả lời hoặc hết giờ)").append("\n\n");
        }

        BigDecimal totalMax = rubrics.stream().map(Rubric::getMaxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        JsonArray rubricsArray = new JsonArray();
        for (Rubric r : rubrics) {
            JsonObject ro = new JsonObject();
            ro.addProperty("rubric_id", r.getId());
            ro.addProperty("criterion", r.getCriterion());
            ro.addProperty("keywords", r.getKeywords() != null ? r.getKeywords() : "");
            ro.addProperty("max_score", r.getMaxScore().doubleValue());
            if (totalMax.signum() > 0) {
                ro.addProperty("weight_percent", r.getMaxScore().multiply(BigDecimal.valueOf(100))
                        .divide(totalMax, 1, RoundingMode.HALF_UP).doubleValue());
            }
            rubricsArray.add(ro);
        }

        String prompt = "Bạn là Giám khảo AI chấm thi vấn đáp môn " + session.getSubject().getName() + " (" + session.getSubject().getCode() + ").\n"
                + "Nhiệm vụ: Phân tích kỹ toàn bộ biên bản hỏi-đáp (Transcript) của sinh viên dưới đây và đánh giá theo từng tiêu chí Rubric.\n"
                + "Bạn chỉ GỢI Ý điểm; giảng viên là người quyết định điểm cuối cùng.\n\n"
                + "DANH SÁCH TIÊU CHÍ RUBRIC (JSON, weight_percent là trọng số % của tiêu chí):\n" + rubricsArray + "\n\n"
                + "BIÊN BẢN HỎI ĐÁP (TRANSCRIPT):\n" + transcriptBuilder + "\n\n"
                + "QUY TẮC ĐÁNH GIÁ:\n"
                + "1. Với mỗi tiêu chí, cho 'score_percent' từ 0 đến 100 = mức độ sinh viên đáp ứng tiêu chí đó.\n"
                + "2. Chỉ dựa vào nội dung sinh viên thực sự nói trong transcript; xét đúng trọng tâm và các từ khoá (keywords).\n"
                + "3. 'comment': 1-2 câu nêu sinh viên đã đạt hoặc còn thiếu gì ở tiêu chí đó.\n"
                + "4. 'evidence': trích NGUYÊN VĂN một đoạn ngắn từ câu trả lời của sinh viên làm bằng chứng; để chuỗi rỗng nếu không có.\n"
                + "5. 'strengths' và 'weaknesses': danh sách điểm mạnh, điểm yếu của sinh viên.\n"
                + "6. 'overall_feedback': 2-3 câu tổng kết mức độ hiểu bài.\n"
                + "7. Trả về DUY NHẤT một đối tượng JSON theo mẫu:\n"
                + "{\n"
                + "  \"overall_feedback\": \"...\",\n"
                + "  \"strengths\": [\"...\"],\n"
                + "  \"weaknesses\": [\"...\"],\n"
                + "  \"evaluations\": [\n"
                + "    {\"rubric_id\": 1, \"score_percent\": 80, \"comment\": \"...\", \"evidence\": \"...\"}\n"
                + "  ]\n"
                + "}";

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        parts.add(partObj);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        requestBody.add("contents", contents);

        // Cùng kiểu cấu hình với chức năng sinh câu hỏi: yêu cầu Gemini trả JSON thuần.
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.2);
        genConfig.addProperty("maxOutputTokens", 4096);
        genConfig.addProperty("candidateCount", 1);
        JsonObject textFormat = new JsonObject();
        textFormat.addProperty("mimeType", "APPLICATION_JSON");
        JsonObject format = new JsonObject();
        format.add("text", textFormat);
        genConfig.add("responseFormat", format);
        requestBody.add("generationConfig", genConfig);

        String rawResponse = transport.post(requestBody.toString(), aiConfiguration.getApiKey());
        return parseGeminiResponse(rawResponse, rubrics);
    }

    private GradingResult parseGeminiResponse(String rawResponse, List<Rubric> rubrics) {
        JsonObject root = JsonParser.parseString(rawResponse).getAsJsonObject();
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Không có candidates từ phản hồi Gemini.");
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonArray parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
        StringBuilder textBuf = new StringBuilder();
        for (JsonElement p : parts) {
            JsonObject po = p.getAsJsonObject();
            if (po.has("thought") && po.get("thought").getAsBoolean()) continue;
            if (po.has("text")) {
                textBuf.append(po.get("text").getAsString());
            }
        }

        String jsonText = textBuf.toString().trim();
        if (jsonText.startsWith("```json")) {
            jsonText = jsonText.substring(7);
        }
        if (jsonText.startsWith("```")) {
            jsonText = jsonText.substring(3);
        }
        if (jsonText.endsWith("```")) {
            jsonText = jsonText.substring(0, jsonText.length() - 3);
        }
        jsonText = jsonText.trim();

        Map<Integer, Rubric> rubricById = new HashMap<>();
        for (Rubric r : rubrics) {
            rubricById.put(r.getId(), r);
        }

        JsonObject parsed = JsonParser.parseString(jsonText).getAsJsonObject();
        String overall = parsed.has("overall_feedback") ? parsed.get("overall_feedback").getAsString()
                : "AI đã hoàn tất đánh giá dựa trên tiêu chí môn học.";
        List<String> strengths = stringList(parsed, "strengths");
        List<String> weaknesses = stringList(parsed, "weaknesses");
        StringBuilder feedback = new StringBuilder(overall.trim());
        if (!strengths.isEmpty()) {
            feedback.append("\nĐiểm mạnh: ").append(String.join("; ", strengths)).append('.');
        }
        if (!weaknesses.isEmpty()) {
            feedback.append("\nĐiểm yếu: ").append(String.join("; ", weaknesses)).append('.');
        }

        Map<Integer, RubricItemEvaluation> evalMap = new HashMap<>();
        if (parsed.has("evaluations") && parsed.get("evaluations").isJsonArray()) {
            for (JsonElement e : parsed.getAsJsonArray("evaluations")) {
                if (!e.isJsonObject()) continue;
                JsonObject eo = e.getAsJsonObject();
                int rubricId = eo.has("rubric_id") ? eo.get("rubric_id").getAsInt() : -1;
                Rubric rubric = rubricById.get(rubricId);
                if (rubric == null) continue;

                BigDecimal score;
                if (eo.has("score_percent")) {
                    double pct = Math.max(0, Math.min(100, eo.get("score_percent").getAsDouble()));
                    score = rubric.getMaxScore().multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    // Tương thích định dạng cũ: điểm tuyệt đối theo max_score.
                    score = BigDecimal.valueOf(eo.has("ai_score") ? eo.get("ai_score").getAsDouble() : 0.0);
                }
                String comment = eo.has("comment") ? eo.get("comment").getAsString().trim() : "";
                String evidence = eo.has("evidence") && !eo.get("evidence").isJsonNull() ? eo.get("evidence").getAsString().trim() : "";
                if (!evidence.isEmpty()) {
                    comment = comment + (comment.isEmpty() ? "" : " ") + "Bằng chứng: “" + evidence + "”";
                }
                evalMap.put(rubricId, new RubricItemEvaluation(score, comment));
            }
        }

        return new GradingResult(feedback.toString(), evalMap);
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> out = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                if (e.isJsonPrimitive() && !e.getAsString().trim().isEmpty()) {
                    out.add(e.getAsString().trim());
                }
            }
        }
        return out;
    }

    private GradingResult evaluateWithRuleBased(List<Rubric> rubrics, List<InterviewTurn> turns, String geminiError) {
        StringBuilder allAnswers = new StringBuilder();
        int answeredCount = 0;
        int totalTurns = turns.size();

        for (InterviewTurn t : turns) {
            if (t.getTranscript() != null && !t.getTranscript().trim().isEmpty()) {
                allAnswers.append(t.getTranscript().toLowerCase()).append(" ");
                answeredCount++;
            }
        }

        String fullText = allAnswers.toString();
        Map<Integer, RubricItemEvaluation> evalMap = new HashMap<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        for (Rubric r : rubrics) {
            BigDecimal max = r.getMaxScore();
            String keywordsStr = r.getKeywords();
            double matchRatio = 0.0;

            if (keywordsStr != null && !keywordsStr.trim().isEmpty()) {
                String[] kwList = keywordsStr.split("[,;]");
                int matched = 0;
                int totalKw = 0;
                for (String kw : kwList) {
                    String clean = kw.trim().toLowerCase();
                    if (!clean.isEmpty()) {
                        totalKw++;
                        if (fullText.contains(clean)) {
                            matched++;
                        }
                    }
                }
                if (totalKw > 0) {
                    matchRatio = (double) matched / totalKw;
                }
            } else {
                // If no keywords specified, base on answer presence and turn completion
                matchRatio = totalTurns > 0 ? ((double) answeredCount / totalTurns) * 0.75 : 0.5;
            }

            // Cap between 0.2 and 1.0 if answered, or 0 if nothing answered
            if (answeredCount == 0) {
                matchRatio = 0.0;
            } else if (matchRatio < 0.2) {
                matchRatio = 0.25;
            }

            BigDecimal awarded = max.multiply(BigDecimal.valueOf(matchRatio)).setScale(2, RoundingMode.HALF_UP);
            String comment;
            if (matchRatio >= 0.75) {
                comment = "Sinh viên nắm chắc kiến thức và thể hiện đầy đủ các thuật ngữ then chốt của tiêu chí này.";
                strengths.add(r.getCriterion());
            } else if (matchRatio >= 0.4) {
                comment = "Sinh viên đã nêu được một phần nội dung nhưng chưa phân tích sâu hoặc còn thiếu một số ý chính.";
            } else {
                comment = "Câu trả lời còn sơ sài, chưa đề cập đầy đủ các từ khóa trọng tâm của tiêu chí.";
                weaknesses.add(r.getCriterion());
            }

            evalMap.put(r.getId(), new RubricItemEvaluation(awarded, comment));
        }

        String overall = String.format(
                "[Chấm dự phòng theo từ khoá – Gemini chưa phản hồi%s] Sinh viên hoàn thành %d/%d lượt trả lời. %s %s",
                geminiError != null ? ": " + geminiError : "",
                answeredCount,
                totalTurns,
                strengths.isEmpty() ? "" : "Thể hiện tốt ở các tiêu chí: " + String.join(", ", strengths) + ".",
                weaknesses.isEmpty() ? "" : "Cần củng cố thêm phần: " + String.join(", ", weaknesses) + "."
        ).trim();

        return new GradingResult(overall, evalMap);
    }

    private static class GradingResult {
        final String overallFeedback;
        final Map<Integer, RubricItemEvaluation> evaluations;

        GradingResult(String overallFeedback, Map<Integer, RubricItemEvaluation> evaluations) {
            this.overallFeedback = overallFeedback;
            this.evaluations = evaluations;
        }
    }

    private static class RubricItemEvaluation {
        final BigDecimal score;
        final String comment;

        RubricItemEvaluation(BigDecimal score, String comment) {
            this.score = score;
            this.comment = comment;
        }
    }
}
