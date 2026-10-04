package com.aives.dao;

import com.aives.model.Rubric;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Feature 1 - CRUD rubric (tiêu chí + thang điểm) của câu hỏi. */
public class RubricDAO {

    public List<Rubric> findByQuestion(int questionId) throws SQLException {
        List<Rubric> list = new ArrayList<>();
        String sql = "SELECT id, question_id, criterion, keywords, max_score, sort_order FROM rubrics"
                + " WHERE question_id = ? ORDER BY sort_order, id";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Rubric findById(int id) throws SQLException {
        String sql = "SELECT id, question_id, criterion, keywords, max_score, sort_order FROM rubrics WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Rubric r) throws SQLException {
        String sql = "INSERT INTO rubrics (question_id, criterion, keywords, max_score, sort_order) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getQuestionId());
            ps.setString(2, r.getCriterion());
            ps.setString(3, r.getKeywords());
            ps.setBigDecimal(4, r.getMaxScore());
            ps.setInt(5, r.getSortOrder());
            ps.executeUpdate();
        }
    }

    public void update(Rubric r) throws SQLException {
        String sql = "UPDATE rubrics SET criterion = ?, keywords = ?, max_score = ?, sort_order = ? WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getCriterion());
            ps.setString(2, r.getKeywords());
            ps.setBigDecimal(3, r.getMaxScore());
            ps.setInt(4, r.getSortOrder());
            ps.setInt(5, r.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rubrics WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Rubric map(ResultSet rs) throws SQLException {
        Rubric r = new Rubric();
        r.setId(rs.getInt("id"));
        r.setQuestionId(rs.getInt("question_id"));
        r.setCriterion(rs.getString("criterion"));
        r.setKeywords(rs.getString("keywords"));
        r.setMaxScore(rs.getBigDecimal("max_score"));
        r.setSortOrder(rs.getInt("sort_order"));
        return r;
    }
}
