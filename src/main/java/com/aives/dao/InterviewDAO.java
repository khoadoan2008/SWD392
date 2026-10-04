package com.aives.dao;

import com.aives.model.InterviewSession;
import com.aives.model.InterviewTurn;
import com.aives.model.Question;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Feature 3 - Lượt phỏng vấn AI và từng lượt hỏi-đáp. */
public class InterviewDAO {

    private static final String SESSION_SELECT =
            "SELECT se.id, se.student_id, u.full_name AS student_name, u.student_code, se.subject_id,"
            + " s.code AS subject_code, se.status, se.answer_time_limit_sec, se.max_followups,"
            + " se.started_at, se.ended_at"
            + " FROM interview_sessions se JOIN users u ON u.id = se.student_id"
            + " JOIN subjects s ON s.id = se.subject_id";

    private static final String TURN_SELECT =
            "SELECT id, session_id, question_id, parent_turn_id, turn_type, main_index, followup_index,"
            + " question_text, transcript, response_time_sec, asked_at, answered_at FROM interview_turns";

    /** Tạo lượt thi + các câu hỏi chính (MAIN) trong một transaction. */
    public int createSession(InterviewSession se, List<Question> mainQuestions) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int sessionId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO interview_sessions (student_id, subject_id, status, answer_time_limit_sec, max_followups)"
                        + " VALUES (?, ?, 'IN_PROGRESS', ?, ?)", new String[] {"id"})) {
                    ps.setInt(1, se.getStudentId());
                    ps.setInt(2, se.getSubjectId());
                    ps.setInt(3, se.getAnswerTimeLimitSec());
                    ps.setInt(4, se.getMaxFollowups());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        sessionId = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO interview_turns (session_id, question_id, turn_type, main_index, followup_index, question_text)"
                        + " VALUES (?, ?, 'MAIN', ?, 0, ?)")) {
                    int index = 1;
                    for (Question q : mainQuestions) {
                        ps.setInt(1, sessionId);
                        ps.setInt(2, q.getId());
                        ps.setInt(3, index++);
                        ps.setString(4, q.getContent());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return sessionId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public InterviewSession findSession(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(SESSION_SELECT + " WHERE se.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSession(rs) : null;
            }
        }
    }

    public InterviewSession findInProgress(int studentId, int subjectId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(SESSION_SELECT
                     + " WHERE se.student_id = ? AND se.subject_id = ? AND se.status = 'IN_PROGRESS'")) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSession(rs) : null;
            }
        }
    }

    /**
     * @param studentId  khác null: chỉ lượt thi của sinh viên đó
     * @param lecturerId khác null: chỉ các môn giảng viên được phân công
     */
    public List<InterviewSession> search(Integer studentId, Integer lecturerId, Integer subjectId) throws SQLException {
        StringBuilder sql = new StringBuilder(SESSION_SELECT).append(" WHERE 1=1");
        List<Integer> params = new ArrayList<>();
        if (studentId != null) {
            sql.append(" AND se.student_id = ?");
            params.add(studentId);
        }
        if (lecturerId != null) {
            sql.append(" AND se.subject_id IN (SELECT subject_id FROM lecturer_subjects WHERE lecturer_id = ?)");
            params.add(lecturerId);
        }
        if (subjectId != null) {
            sql.append(" AND se.subject_id = ?");
            params.add(subjectId);
        }
        sql.append(" ORDER BY se.started_at DESC");
        List<InterviewSession> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setInt(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSession(rs));
                }
            }
        }
        return list;
    }

    public List<InterviewTurn> findTurns(int sessionId) throws SQLException {
        List<InterviewTurn> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(TURN_SELECT
                     + " WHERE session_id = ? ORDER BY main_index, followup_index")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTurn(rs));
                }
            }
        }
        return list;
    }

    /** Lượt hỏi kế tiếp chưa được trả lời (null = đã hết câu hỏi). */
    public InterviewTurn findCurrentTurn(int sessionId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(TURN_SELECT
                     + " WHERE session_id = ? AND answered_at IS NULL ORDER BY main_index, followup_index LIMIT 1")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTurn(rs) : null;
            }
        }
    }

    public InterviewTurn findTurn(int turnId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(TURN_SELECT + " WHERE id = ?")) {
            ps.setInt(1, turnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTurn(rs) : null;
            }
        }
    }

    /** Ghi nhận thời điểm câu hỏi được đọc lần đầu (để tính thời gian trả lời). */
    public void markAsked(int turnId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE interview_turns SET asked_at = LOCALTIMESTAMP WHERE id = ? AND asked_at IS NULL")) {
            ps.setInt(1, turnId);
            ps.executeUpdate();
        }
    }

    /** Lưu transcript; thời gian trả lời tính phía server từ asked_at. */
    public void saveAnswer(int turnId, String transcript) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE interview_turns SET transcript = ?, answered_at = LOCALTIMESTAMP,"
                     + " response_time_sec = CAST(EXTRACT(EPOCH FROM LOCALTIMESTAMP - COALESCE(asked_at, LOCALTIMESTAMP)) AS INT)"
                     + " WHERE id = ? AND answered_at IS NULL")) {
            ps.setString(1, transcript);
            ps.setInt(2, turnId);
            ps.executeUpdate();
        }
    }

    public void insertFollowUp(InterviewTurn t) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO interview_turns (session_id, question_id, parent_turn_id, turn_type, main_index,"
                     + " followup_index, question_text) VALUES (?, ?, ?, 'FOLLOW_UP', ?, ?, ?)")) {
            ps.setInt(1, t.getSessionId());
            if (t.getQuestionId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, t.getQuestionId());
            }
            ps.setInt(3, t.getParentTurnId());
            ps.setInt(4, t.getMainIndex());
            ps.setInt(5, t.getFollowupIndex());
            ps.setString(6, t.getQuestionText());
            ps.executeUpdate();
        }
    }

    public void finishSession(int sessionId, String status) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE interview_sessions SET status = ?, ended_at = LOCALTIMESTAMP WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }

    /** Các turn bị xoá theo (ON DELETE CASCADE). */
    public void deleteSession(int sessionId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM interview_sessions WHERE id = ?")) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        }
    }

    private InterviewSession mapSession(ResultSet rs) throws SQLException {
        InterviewSession se = new InterviewSession();
        se.setId(rs.getInt("id"));
        se.setStudentId(rs.getInt("student_id"));
        se.setStudentName(rs.getString("student_name"));
        se.setStudentCode(rs.getString("student_code"));
        se.setSubjectId(rs.getInt("subject_id"));
        se.setSubjectCode(rs.getString("subject_code"));
        se.setStatus(rs.getString("status"));
        se.setAnswerTimeLimitSec(rs.getInt("answer_time_limit_sec"));
        se.setMaxFollowups(rs.getInt("max_followups"));
        se.setStartedAt(rs.getTimestamp("started_at"));
        se.setEndedAt(rs.getTimestamp("ended_at"));
        return se;
    }

    private InterviewTurn mapTurn(ResultSet rs) throws SQLException {
        InterviewTurn t = new InterviewTurn();
        t.setId(rs.getInt("id"));
        t.setSessionId(rs.getInt("session_id"));
        int qid = rs.getInt("question_id");
        t.setQuestionId(rs.wasNull() ? null : qid);
        int parent = rs.getInt("parent_turn_id");
        t.setParentTurnId(rs.wasNull() ? null : parent);
        t.setTurnType(rs.getString("turn_type"));
        t.setMainIndex(rs.getInt("main_index"));
        t.setFollowupIndex(rs.getInt("followup_index"));
        t.setQuestionText(rs.getString("question_text"));
        t.setTranscript(rs.getString("transcript"));
        int rt = rs.getInt("response_time_sec");
        t.setResponseTimeSec(rs.wasNull() ? null : rt);
        t.setAskedAt(rs.getTimestamp("asked_at"));
        t.setAnsweredAt(rs.getTimestamp("answered_at"));
        return t;
    }
}
