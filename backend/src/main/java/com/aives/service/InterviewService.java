package com.aives.service;

import com.aives.dto.request.GradeSessionRequest;
import com.aives.dto.response.InterviewSessionResponse;
import com.aives.dto.response.InterviewTurnDataResponse;
import com.aives.dto.response.StudentHomeResponse;
import com.aives.dto.response.SubjectResponse;
import com.aives.exception.GradingException;
import com.aives.model.entity.*;
import com.aives.model.enums.QuestionStatus;
import com.aives.model.enums.Role;
import com.aives.model.enums.SessionStatus;
import com.aives.model.enums.TurnType;
import com.aives.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewTurnRepository turnRepository;
    private final QuestionRepository questionRepository;
    private final RubricRepository rubricRepository;
    private final SubjectRepository subjectRepository;
    private final SystemConfigService configService;
    private final FollowUpGenerator followUpGenerator;
    private final AiGradingService aiGradingService;
    private final SessionRubricScoreRepository sessionRubricScoreRepository;
    private final AiGradingTrigger aiGradingTrigger;

    @Transactional
    public InterviewSessionResponse startSession(User student, Integer subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + subjectId));

        // If in-progress session exists, return it
        Optional<InterviewSession> existing = sessionRepository
                .findFirstByStudentIdAndSubjectIdAndStatusOrderByIdDesc(student.getId(), subjectId, SessionStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            return InterviewSessionResponse.of(existing.get());
        }

        int mainCount = configService.getIntConfig(SystemConfig.MAIN_QUESTIONS, 3);
        List<Question> questions = questionRepository.findRandomApprovedBySubject(subjectId, PageRequest.of(0, mainCount));
        if (questions.isEmpty()) {
            throw new IllegalStateException("Môn học này hiện chưa có câu hỏi nào được duyệt vào ngân hàng.");
        }

        int answerTime = configService.getIntConfig(SystemConfig.ANSWER_TIME, 120);
        int maxFollowups = configService.getIntConfig(SystemConfig.MAX_FOLLOWUPS, 2);

        InterviewSession session = InterviewSession.builder()
                .student(student)
                .subject(subject)
                .status(SessionStatus.IN_PROGRESS)
                .answerTimeLimitSec(answerTime)
                .maxFollowups(maxFollowups)
                .startedAt(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);

        List<InterviewTurn> turns = new ArrayList<>();
        int index = 1;
        for (Question q : questions) {
            InterviewTurn turn = InterviewTurn.builder()
                    .session(session)
                    .question(q)
                    .turnType(TurnType.MAIN)
                    .mainIndex(index++)
                    .followupIndex(0)
                    .questionText(q.getContent())
                    .build();
            turns.add(turn);
        }
        turnRepository.saveAll(turns);

        session.setTurns(turns);
        return InterviewSessionResponse.of(session);
    }

    @Transactional
    public InterviewTurnDataResponse getCurrentTurnData(Integer sessionId, User student) {
        InterviewSession session = getSessionById(sessionId);
        if (!session.getStudent().getId().equals(student.getId()) && student.getRole() != Role.ADMIN) {
            throw new SecurityException("Bạn không có quyền truy cập lượt thi này.");
        }

        InterviewSessionResponse.TurnSummary currentTurnDto = null;
        InterviewSessionResponse.TurnSummary prevTurnDto = null;
        Long remaining = null;

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            Optional<InterviewTurn> currentOpt = turnRepository.findCurrentTurn(sessionId);
            if (currentOpt.isPresent()) {
                InterviewTurn current = currentOpt.get();
                if (current.getAskedAt() == null) {
                    current.setAskedAt(LocalDateTime.now());
                    current = turnRepository.save(current);
                }
                long elapsed = Duration.between(current.getAskedAt(), LocalDateTime.now()).getSeconds();
                remaining = Math.max(0, session.getAnswerTimeLimitSec() - elapsed);
                currentTurnDto = InterviewSessionResponse.TurnSummary.of(current);

                if (current.isFollowUp()) {
                    List<InterviewTurn> allTurns = turnRepository.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(sessionId);
                    InterviewTurn prev = null;
                    for (InterviewTurn t : allTurns) {
                        if (Objects.equals(t.getMainIndex(), current.getMainIndex()) && t.getAnsweredAt() != null) {
                            prev = t;
                        }
                    }
                    prevTurnDto = InterviewSessionResponse.TurnSummary.of(prev);
                }
            } else {
                session.setStatus(SessionStatus.COMPLETED);
                session.setEndedAt(LocalDateTime.now());
                session = sessionRepository.save(session);
                aiGradingTrigger.gradeAfterCommit(session.getId());
            }
        }
        String speechLang = configService.getConfig(SystemConfig.SPEECH_LANGUAGE, "vi-VN");
        return new InterviewTurnDataResponse(
                InterviewSessionResponse.of(session),
                currentTurnDto,
                prevTurnDto,
                remaining,
                speechLang
        );
    }

    @Transactional
    public void submitAnswer(Integer sessionId, Integer turnId, String transcript, User student) {
        InterviewSession session = getSessionById(sessionId);
        if (!session.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("Bạn không có quyền gửi câu trả lời cho lượt thi này.");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return;
        }

        InterviewTurn current = turnRepository.findById(turnId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt câu hỏi ID: " + turnId));
        if (!current.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Lượt câu hỏi không thuộc về phiên thi này.");
        }
        if (current.getAnsweredAt() != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        current.setTranscript(transcript);
        current.setAnsweredAt(now);
        if (current.getAskedAt() != null) {
            current.setResponseTimeSec((int) Duration.between(current.getAskedAt(), now).getSeconds());
        }
        turnRepository.save(current);

        // Check if follow-up question should be triggered
        if (current.getFollowupIndex() < session.getMaxFollowups()) {
            List<InterviewTurn> allTurns = turnRepository.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(sessionId);
            List<InterviewTurn> thread = new ArrayList<>();
            InterviewTurn mainTurn = null;
            for (InterviewTurn t : allTurns) {
                if (Objects.equals(t.getMainIndex(), current.getMainIndex()) && t.getAnsweredAt() != null) {
                    thread.add(t);
                    if (!t.isFollowUp()) {
                        mainTurn = t;
                    }
                }
            }

            if (mainTurn != null) {
                List<Rubric> rubrics = (session.getSubject() != null)
                        ? rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(session.getSubject().getId())
                        : Collections.emptyList();

                String followUpText = followUpGenerator.generate(mainTurn.getQuestionText(), rubrics, thread);
                if (followUpText != null && !followUpText.trim().isEmpty()) {
                    InterviewTurn followUpTurn = InterviewTurn.builder()
                            .session(session)
                            .question(mainTurn.getQuestion())
                            .parentTurn(mainTurn)
                            .turnType(TurnType.FOLLOW_UP)
                            .mainIndex(current.getMainIndex())
                            .followupIndex(current.getFollowupIndex() + 1)
                            .questionText(followUpText)
                            .build();
                    turnRepository.save(followUpTurn);
                }
            }
        }

        // Check if there are any remaining unanswered turns
        if (turnRepository.findCurrentTurn(sessionId).isEmpty()) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(LocalDateTime.now());
            session = sessionRepository.save(session);
            aiGradingTrigger.gradeAfterCommit(session.getId());
        }
    }

    @Transactional
    public void abortSession(Integer sessionId, User student) {
        InterviewSession session = getSessionById(sessionId);
        if (!session.getStudent().getId().equals(student.getId()) && student.getRole() != Role.ADMIN) {
            throw new SecurityException("Không có quyền huỷ lượt thi.");
        }
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            session.setStatus(SessionStatus.ABORTED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    @Transactional(readOnly = true)
    public StudentHomeResponse getStudentHomeData(User student) {
        List<SubjectResponse> activeSubjects = subjectRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(SubjectResponse::of).toList();
        List<InterviewSessionResponse> history = getStudentHistory(student.getId());
        return new StudentHomeResponse(activeSubjects, history);
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getStudentHistory(Integer studentId) {
        return sessionRepository.findByStudentIdOrderByIdDesc(studentId).stream()
                .map(InterviewSessionResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> searchSessions(User currentUser, Integer subjectId) {
        List<Integer> allowedSubjects = (currentUser.getRole() == Role.ADMIN) ? null :
                currentUser.getAssignedSubjects().stream().map(Subject::getId).toList();
        return sessionRepository.search(null, allowedSubjects, subjectId).stream()
                .map(InterviewSessionResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countSessionStats(User currentUser) {
        List<Integer> allowedSubjects = (currentUser.getRole() == Role.ADMIN) ? null :
                currentUser.getAssignedSubjects().stream().map(Subject::getId).toList();
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL", 0L);
        for (SessionStatus s : SessionStatus.values()) {
            stats.put(s.name(), 0L);
        }
        List<Object[]> rows = sessionRepository.countByStatus(allowedSubjects);
        long total = 0;
        for (Object[] r : rows) {
            SessionStatus st = (SessionStatus) r[0];
            Long count = (Long) r[1];
            stats.put(st.name(), count);
            total += count;
        }
        stats.put("TOTAL", total);
        return stats;
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getSessionDetail(Integer sessionId) {
        return InterviewSessionResponse.of(getSessionById(sessionId));
    }

    @Transactional(readOnly = true)
    public InterviewSession getSessionById(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi ID: " + sessionId));
    }

    @Transactional
    public void deleteSession(Integer sessionId, User currentUser) {
        InterviewSession session = getSessionById(sessionId);
        if (currentUser.getRole() != Role.ADMIN) {
            if (session.getSubject() != null &&
                    currentUser.getAssignedSubjects().stream().noneMatch(s -> s.getId().equals(session.getSubject().getId()))) {
                throw new SecurityException("Không có quyền xoá lượt thi môn học này.");
            }
        }
        sessionRepository.delete(session);
    }

    @Transactional
    public InterviewSessionResponse gradeSession(Integer sessionId, GradeSessionRequest request, User lecturer) {
        InterviewSession session = findSessionForGrading(sessionId);
        checkLecturerPermission(session, lecturer);
        checkGradable(session);
        validateRubricGrades(session, request);

        session.setFinalScore(request.getFinalScore());
        session.setLecturerFeedback(request.getLecturerFeedback());
        session.setGradedBy(lecturer);
        session.setGradedAt(LocalDateTime.now());

        if (request.getRubricGrades() != null && !request.getRubricGrades().isEmpty()) {
            Map<Integer, SessionRubricScore> existingScores = new HashMap<>();
            List<SessionRubricScore> currentScores = sessionRubricScoreRepository.findBySessionId(sessionId);
            for (SessionRubricScore srs : currentScores) {
                if (srs.getRubric() != null) {
                    existingScores.put(srs.getRubric().getId(), srs);
                }
            }

            for (GradeSessionRequest.RubricGradeItem item : request.getRubricGrades()) {
                SessionRubricScore srs = existingScores.get(item.getRubricId());
                if (srs != null) {
                    if (item.getFinalScore() != null) {
                        srs.setFinalScore(item.getFinalScore());
                    }
                    if (item.getComment() != null) {
                        srs.setComment(item.getComment());
                    }
                    sessionRubricScoreRepository.save(srs);
                } else {
                    Rubric rubric = rubricRepository.findById(item.getRubricId()).orElse(null);
                    if (rubric != null) {
                        SessionRubricScore newSrs = SessionRubricScore.builder()
                                .session(session)
                                .rubric(rubric)
                                .aiScore(BigDecimal.ZERO)
                                .finalScore(item.getFinalScore() != null ? item.getFinalScore() : BigDecimal.ZERO)
                                .comment(item.getComment())
                                .build();
                        sessionRubricScoreRepository.save(newSrs);
                    }
                }
            }
        }

        session = sessionRepository.save(session);
        return InterviewSessionResponse.of(session);
    }

    @Transactional
    public InterviewSessionResponse reEvaluateSession(Integer sessionId, User lecturer) {
        InterviewSession session = findSessionForGrading(sessionId);
        checkLecturerPermission(session, lecturer);
        checkGradable(session);
        session = aiGradingService.evaluateSession(session);
        return InterviewSessionResponse.of(session);
    }

    private InterviewSession findSessionForGrading(Integer sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GradingException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt thi ID: " + sessionId));
    }

    private void checkLecturerPermission(InterviewSession session, User lecturer) {
        if (lecturer.getRole() != Role.ADMIN) {
            if (session.getSubject() != null &&
                    lecturer.getAssignedSubjects().stream().noneMatch(s -> s.getId().equals(session.getSubject().getId()))) {
                throw new GradingException(HttpStatus.FORBIDDEN, "Không có quyền chấm điểm lượt thi môn học này.");
            }
        }
    }

    /** Chỉ chấm khi sinh viên đã thi xong (hoặc kết thúc sớm) và điểm chưa bị khoá. */
    private void checkGradable(InterviewSession session) {
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new GradingException(HttpStatus.CONFLICT, "Sinh viên chưa hoàn thành buổi thi, chưa thể chấm điểm.");
        }
        if (session.isGraded()) {
            throw new GradingException(HttpStatus.CONFLICT,
                    "Điểm chính thức của lượt thi này đã được khoá, không thể chỉnh sửa hay yêu cầu AI chấm lại.");
        }
    }

    /** Mỗi tiêu chí phải thuộc rubric của môn thi và điểm không vượt quá điểm tối đa của tiêu chí. */
    private void validateRubricGrades(InterviewSession session, GradeSessionRequest request) {
        if (request.getRubricGrades() == null) {
            return;
        }
        Integer subjectId = session.getSubject() != null ? session.getSubject().getId() : null;
        for (GradeSessionRequest.RubricGradeItem item : request.getRubricGrades()) {
            Rubric rubric = rubricRepository.findById(item.getRubricId())
                    .orElseThrow(() -> new GradingException(HttpStatus.BAD_REQUEST, "Không tìm thấy tiêu chí rubric ID: " + item.getRubricId()));
            if (rubric.getSubject() == null || !Objects.equals(rubric.getSubject().getId(), subjectId)) {
                throw new GradingException(HttpStatus.BAD_REQUEST, "Tiêu chí \"" + rubric.getCriterion() + "\" không thuộc môn học của lượt thi.");
            }
            if (item.getFinalScore() != null && item.getFinalScore().compareTo(rubric.getMaxScore()) > 0) {
                throw new GradingException(HttpStatus.BAD_REQUEST, "Điểm tiêu chí \"" + rubric.getCriterion()
                        + "\" không được vượt quá " + rubric.getMaxScore().stripTrailingZeros().toPlainString() + " điểm.");
            }
        }
    }
}
