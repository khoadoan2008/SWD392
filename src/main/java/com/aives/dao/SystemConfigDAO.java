package com.aives.dao;

import com.aives.model.SystemConfig;
import com.aives.util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Feature 7 - Cấu hình hệ thống (ngôn ngữ STT/TTS, giới hạn phỏng vấn). */
public class SystemConfigDAO {

    public List<SystemConfig> findAll() throws SQLException {
        List<SystemConfig> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT config_key, config_value, description FROM system_configs ORDER BY config_key")) {
            while (rs.next()) {
                SystemConfig cfg = new SystemConfig();
                cfg.setKey(rs.getString(1));
                cfg.setValue(rs.getString(2));
                cfg.setDescription(rs.getString(3));
                list.add(cfg);
            }
        }
        return list;
    }

    public String get(String key, String defaultValue) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT config_value FROM system_configs WHERE config_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        }
    }

    public int getInt(String key, int defaultValue) throws SQLException {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void update(String key, String value) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE system_configs SET config_value = ? WHERE config_key = ?")) {
            ps.setString(1, value);
            ps.setString(2, key);
            ps.executeUpdate();
        }
    }
}
