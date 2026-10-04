package com.aives.dao;

import com.aives.model.BloomLevel;
import com.aives.model.Question;
import com.aives.model.QuestionSource;
import com.aives.model.QuestionStatus;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Feature 1 - CRUD ngân hàng câu hỏi. */
public class QuestionDAO {

    private static final String SELECT =
            "SELECT q.id, q.subject_id, s.code AS subject_code, q.topic, q.content, q.reference_answer,"
            + " q.bloom_level, q.status, q.source, q.created_by, u.full_name AS created_by_name,"
            + " q.reviewed_by, q.created_at, q.updated_at"
            + " FROM questions q JOIN subjects s ON s.id = q.subject_id JOIN users u ON u.id = q.created_by";

    /**
     * Lọc câu hỏi.
     *
     * @param lecturerId nếu khác null: chỉ lấy câu hỏi thuộc các môn giảng viên được phân công
     */
    public List<Question> search(Integer lecturerId, Integer subjectId, QuestionStatus status,
                                 BloomLevel bloom, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (lecturerId != null) {
            sql.append(" AND q.subject_id IN (SELECT subject_id FROM lecturer_subjects WHERE lecturer_id = ?)");
            params.add(lecturerId);
        }
        if (subjectId != null) {
            sql.append(" AND q.subject_id = ?");
            params.add(subjectId);
        }
        if (status != null) {
            sql.append(" AND q.status = ?");
            params.add(status.name());
        }
        if (bloom != null) {
            sql.append(" AND q.bloom_level = ?");
            params.add(bloom.name());
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (q.content ILIKE ? OR q.topic ILIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY q.updated_at DESC");

        List<Question> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Thống kê số câu hỏi theo trạng thái (key = tên status, thêm key TOTAL) cho thẻ thống kê. */
    public Map<String, Integer> countByStatus(Integer lecturerId) throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM questions"
                + (lecturerId != null
                    ? " WHERE subject_id IN (SELECT subject_id FROM lecturer_subjects WHERE lecturer_id = ?)" : "")
                + " GROUP BY status";
        Map<String, Integer> counts = new HashMap<>();
        for (QuestionStatus s : QuestionStatus.values()) {
            counts.put(s.name(), 0);
        }
        int total = 0;
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (lecturerId != null) {
                ps.setInt(1, lecturerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getInt(2));
                    total += rs.getInt(2);
                }
            }
        }
        counts.put("TOTAL", total);
        return counts;
    }

    public Question findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE q.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Feature 3 dùng: chọn ngẫu nhiên n câu đã duyệt của môn. */
    public List<Question> findRandomApproved(int subjectId, int n) throws SQLException {
        String sql = SELECT + " WHERE q.subject_id = ? AND q.status = 'APPROVED' ORDER BY RANDOM() LIMIT ?";
        List<Question> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public int insert(Question q) throws SQLException {
        String sql = "INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            ps.setInt(1, q.getSubjectId());
            ps.setString(2, q.getTopic());
            ps.setString(3, q.getContent());
            ps.setString(4, q.getReferenceAnswer());
            ps.setString(5, q.getBloomLevel().name());
            ps.setString(6, q.getStatus().name());
            ps.setString(7, q.getSource().name());
            ps.setInt(8, q.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(Question q) throws SQLException {
        String sql = "UPDATE questions SET subject_id = ?, topic = ?, content = ?, reference_answer = ?,"
                + " bloom_level = ?, updated_at = LOCALTIMESTAMP WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, q.getSubjectId());
            ps.setString(2, q.getTopic());
            ps.setString(3, q.getContent());
            ps.setString(4, q.getReferenceAnswer());
            ps.setString(5, q.getBloomLevel().name());
            ps.setInt(6, q.getId());
            ps.executeUpdate();
        }
    }

    /** Duyệt / loại bỏ / gửi duyệt câu hỏi. reviewerId null khi chỉ đổi về nháp. */
    public void updateStatus(int id, QuestionStatus status, Integer reviewerId) throws SQLException {
        String sql = "UPDATE questions SET status = ?, reviewed_by = ?, updated_at = LOCALTIMESTAMP WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (reviewerId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, reviewerId);
            }
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** Rubric bị xoá theo (ON DELETE CASCADE); lượt phỏng vấn cũ giữ snapshot nội dung câu hỏi. */
    public void delete(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Question map(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setSubjectId(rs.getInt("subject_id"));
        q.setSubjectCode(rs.getString("subject_code"));
        q.setTopic(rs.getString("topic"));
        q.setContent(rs.getString("content"));
        q.setReferenceAnswer(rs.getString("reference_answer"));
        q.setBloomLevel(BloomLevel.valueOf(rs.getString("bloom_level")));
        q.setStatus(QuestionStatus.valueOf(rs.getString("status")));
        q.setSource(QuestionSource.valueOf(rs.getString("source")));
        q.setCreatedBy(rs.getInt("created_by"));
        q.setCreatedByName(rs.getString("created_by_name"));
        int reviewer = rs.getInt("reviewed_by");
        q.setReviewedBy(rs.wasNull() ? null : reviewer);
        q.setCreatedAt(rs.getTimestamp("created_at"));
        q.setUpdatedAt(rs.getTimestamp("updated_at"));
        return q;
    }
}
