package com.aives.dao;

import com.aives.model.Subject;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    public List<Subject> findAll() throws SQLException {
        return query("SELECT id, code, name, description, active FROM subjects ORDER BY code", null);
    }

    public List<Subject> findActive() throws SQLException {
        return query("SELECT id, code, name, description, active FROM subjects WHERE active = TRUE ORDER BY code", null);
    }

    public List<Subject> findByLecturer(int lecturerId) throws SQLException {
        return query("SELECT s.id, s.code, s.name, s.description, s.active FROM subjects s"
                + " JOIN lecturer_subjects ls ON ls.subject_id = s.id"
                + " WHERE ls.lecturer_id = ? ORDER BY s.code", lecturerId);
    }

    public Subject findById(int id) throws SQLException {
        List<Subject> list = query("SELECT id, code, name, description, active FROM subjects WHERE id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void insert(Subject s) throws SQLException {
        String sql = "INSERT INTO subjects (code, name, description, active) VALUES (?, ?, ?, ?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getCode());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDescription());
            ps.setBoolean(4, s.isActive());
            ps.executeUpdate();
        }
    }

    public void update(Subject s) throws SQLException {
        String sql = "UPDATE subjects SET code = ?, name = ?, description = ?, active = ? WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getCode());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDescription());
            ps.setBoolean(4, s.isActive());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
        }
    }

    /** Ném SQLException nếu môn đã có câu hỏi / lượt thi. */
    public void delete(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private List<Subject> query(String sql, Integer param) throws SQLException {
        List<Subject> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject();
                    s.setId(rs.getInt("id"));
                    s.setCode(rs.getString("code"));
                    s.setName(rs.getString("name"));
                    s.setDescription(rs.getString("description"));
                    s.setActive(rs.getBoolean("active"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
