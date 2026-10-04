package com.aives.dao;

import com.aives.model.Role;
import com.aives.model.User;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Feature 7 - CRUD tài khoản + phân quyền giảng viên/môn học. */
public class UserDAO {

    private static final String COLUMNS =
            "id, username, password_hash, full_name, email, role, student_code, active, created_at";

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Tìm kiếm theo vai trò (null = tất cả) và từ khoá (username / họ tên / MSSV). */
    public List<User> search(Role role, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (username ILIKE ? OR full_name ILIKE ? OR student_code ILIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY role, username");
        List<User> list = new ArrayList<>();
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

    /** Thống kê tài khoản: key ADMIN / LECTURER / STUDENT / LOCKED / TOTAL. */
    public Map<String, Integer> countByRole() throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        for (Role r : Role.values()) {
            counts.put(r.name(), 0);
        }
        int total = 0;
        int locked = 0;
        try (Connection c = DBContext.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT role, active, COUNT(*) FROM users GROUP BY role, active")) {
            while (rs.next()) {
                int n = rs.getInt(3);
                counts.merge(rs.getString(1), n, Integer::sum);
                total += n;
                if (!rs.getBoolean(2)) {
                    locked += n;
                }
            }
        }
        counts.put("LOCKED", locked);
        counts.put("TOTAL", total);
        return counts;
    }

    public int count() throws SQLException {
        try (Connection c = DBContext.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int insert(User u) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role, student_code, active)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getRole().name());
            ps.setString(6, u.getStudentCode());
            ps.setBoolean(7, u.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Cập nhật thông tin; passwordHash null = giữ nguyên mật khẩu cũ. */
    public void update(User u) throws SQLException {
        boolean changePassword = u.getPasswordHash() != null;
        String sql = "UPDATE users SET full_name = ?, email = ?, role = ?, student_code = ?, active = ?"
                + (changePassword ? ", password_hash = ?" : "") + " WHERE id = ?";
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getFullName());
            ps.setString(i++, u.getEmail());
            ps.setString(i++, u.getRole().name());
            ps.setString(i++, u.getStudentCode());
            ps.setBoolean(i++, u.isActive());
            if (changePassword) {
                ps.setString(i++, u.getPasswordHash());
            }
            ps.setInt(i, u.getId());
            ps.executeUpdate();
        }
    }

    /** Xoá cứng; ném SQLException nếu tài khoản đã có dữ liệu liên quan (câu hỏi, lượt thi). */
    public void delete(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Integer> findSubjectIdsOfLecturer(int lecturerId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT subject_id FROM lecturer_subjects WHERE lecturer_id = ?")) {
            ps.setInt(1, lecturerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    /** Ghi đè toàn bộ danh sách môn được phân công cho giảng viên (trong 1 transaction). */
    public void replaceLecturerSubjects(int lecturerId, List<Integer> subjectIds) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement("DELETE FROM lecturer_subjects WHERE lecturer_id = ?");
                 PreparedStatement ins = c.prepareStatement(
                         "INSERT INTO lecturer_subjects (lecturer_id, subject_id) VALUES (?, ?)")) {
                del.setInt(1, lecturerId);
                del.executeUpdate();
                for (Integer sid : subjectIds) {
                    ins.setInt(1, lecturerId);
                    ins.setInt(2, sid);
                    ins.addBatch();
                }
                ins.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setStudentCode(rs.getString("student_code"));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
