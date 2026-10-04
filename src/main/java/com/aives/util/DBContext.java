package com.aives.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Mở kết nối JDBC tới PostgreSQL, cấu hình đọc từ db.properties. */
public final class DBContext {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBContext.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy db.properties trên classpath");
            }
            PROPS.load(in);
            Class.forName("org.postgresql.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBContext() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(prop("db.url"), prop("db.user"), prop("db.password"));
    }

    /** Cho phép ghi đè bằng JVM option, VD: -Ddb.password=xxx (không cần sửa file). */
    private static String prop(String key) {
        return System.getProperty(key, PROPS.getProperty(key));
    }
}
