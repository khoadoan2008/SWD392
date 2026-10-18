package com.aives.service.impl;

import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.Rubric;
import com.aives.service.FollowUpGenerator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RuleBasedFollowUpGenerator implements FollowUpGenerator {

    private static final int MIN_WORDS = 8;
    private static final String ASK_MORE =
            "Câu trả lời của bạn còn khá ngắn. Bạn có thể giải thích chi tiết hơn và cho một ví dụ cụ thể không?";

    @Override
    public String generate(String mainQuestionText, List<Rubric> rubrics, List<InterviewTurn> thread) {
        if (thread == null || thread.isEmpty()) {
            return null;
        }
        InterviewTurn last = thread.get(thread.size() - 1);
        StringBuilder answers = new StringBuilder();
        StringBuilder askedQuestions = new StringBuilder();
        for (InterviewTurn t : thread) {
            answers.append(' ').append(nullToEmpty(t.getTranscript()));
            askedQuestions.append(' ').append(t.getQuestionText());
        }
        String allAnswers = answers.toString().toLowerCase(Locale.ROOT);
        String allAsked = askedQuestions.toString();

        if (wordCount(last.getTranscript()) < MIN_WORDS && !allAsked.contains(ASK_MORE)) {
            return ASK_MORE;
        }

        if (rubrics != null) {
            for (Rubric r : rubrics) {
                if (r.getKeywords() == null || r.getKeywords().trim().isEmpty()) {
                    continue;
                }
                if (allAsked.contains(r.getCriterion())) {
                    continue; // đã hỏi xoáy tiêu chí này rồi
                }
                boolean covered = false;
                for (String kw : r.getKeywords().split(",")) {
                    String k = kw.trim().toLowerCase(Locale.ROOT);
                    if (!k.isEmpty() && allAnswers.contains(k)) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    return "Bạn chưa đề cập đến trọng tâm \"" + r.getCriterion()
                            + "\". Bạn có thể phân tích sâu hơn về khía cạnh này không?";
                }
            }
        }
        return null;
    }

    private static int wordCount(String s) {
        String t = nullToEmpty(s).trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
