package com.aives.service;

import com.aives.dao.InterviewDAO;
import com.aives.dao.QuestionDAO;
import com.aives.dao.RubricDAO;
import com.aives.dao.SystemConfigDAO;
import com.aives.model.InterviewSession;
import com.aives.model.InterviewTurn;
import com.aives.model.Question;
import com.aives.model.SystemConfig;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Feature 3 - Điều phối luồng phỏng vấn: bắt đầu, nhận câu trả lời, hỏi xoáy, kết thúc. */
public class InterviewService {

    private final InterviewDAO interviewDAO = new InterviewDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final RubricDAO rubricDAO = new RubricDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();
    private final FollowUpGenerator followUpGenerator;

    public InterviewService() {
        this(new RuleBasedFollowUpGenerator());
    }

    public InterviewService(FollowUpGenerator followUpGenerator) {
        this.followUpGenerator = followUpGenerator;
    }

    /** Bắt đầu lượt thi mới, hoặc trả về lượt đang dở của sinh viên cho môn này. */
    public int start(int studentId, int subjectId) throws SQLException {
        InterviewSession existing = interviewDAO.findInProgress(studentId, subjectId);
        if (existing != null) {
            return existing.getId();
        }
        int mainCount = configDAO.getInt(SystemConfig.MAIN_QUESTIONS, 3);
        List<Question> questions = questionDAO.findRandomApproved(subjectId, mainCount);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Môn học này chưa có câu hỏi nào được duyệt.");
        }
        InterviewSession se = new InterviewSession();
        se.setStudentId(studentId);
        se.setSubjectId(subjectId);
        se.setAnswerTimeLimitSec(configDAO.getInt(SystemConfig.ANSWER_TIME, 120));
        se.setMaxFollowups(configDAO.getInt(SystemConfig.MAX_FOLLOWUPS, 2));
        return interviewDAO.createSession(se, questions);
    }

    /**
     * Lưu câu trả lời cho lượt hỏi hiện tại, sinh câu hỏi xoáy nếu cần,
     * và kết thúc lượt thi khi không còn câu hỏi.
     */
    public void submitAnswer(InterviewSession se, int turnId, String transcript) throws SQLException {
        InterviewTurn current = interviewDAO.findCurrentTurn(se.getId());
        if (current == null || current.getId() != turnId) {
            return; // gửi trùng / gửi lại câu cũ -> bỏ qua
        }
        interviewDAO.saveAnswer(turnId, transcript == null ? "" : transcript.trim());

        if (current.getFollowupIndex() < se.getMaxFollowups()) {
            List<InterviewTurn> thread = new ArrayList<>();
            InterviewTurn mainTurn = null;
            for (InterviewTurn t : interviewDAO.findTurns(se.getId())) {
                if (t.getMainIndex() == current.getMainIndex() && t.getAnsweredAt() != null) {
                    thread.add(t);
                    if (!t.isFollowUp()) {
                        mainTurn = t;
                    }
                }
            }
            if (mainTurn != null) {
                String followUp = followUpGenerator.generate(
                        mainTurn.getQuestionText(),
                        mainTurn.getQuestionId() == null
                                ? new ArrayList<>() : rubricDAO.findByQuestion(mainTurn.getQuestionId()),
                        thread);
                if (followUp != null) {
                    InterviewTurn t = new InterviewTurn();
                    t.setSessionId(se.getId());
                    t.setQuestionId(mainTurn.getQuestionId());
                    t.setParentTurnId(mainTurn.getId());
                    t.setMainIndex(current.getMainIndex());
                    t.setFollowupIndex(current.getFollowupIndex() + 1);
                    t.setQuestionText(followUp);
                    interviewDAO.insertFollowUp(t);
                }
            }
        }

        if (interviewDAO.findCurrentTurn(se.getId()) == null) {
            interviewDAO.finishSession(se.getId(), InterviewSession.COMPLETED);
        }
    }
}
