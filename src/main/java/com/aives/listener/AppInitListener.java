package com.aives.listener;

import com.aives.dao.SubjectDAO;
import com.aives.dao.UserDAO;
import com.aives.model.Role;
import com.aives.model.Subject;
import com.aives.model.User;
import com.aives.util.PasswordUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Tạo tài khoản demo khi bảng users còn trống (DB mới chạy script).
 * admin/admin123, lecturer1/123456, student1/123456 - ĐỔI MẬT KHẨU khi triển khai thật.
 */
@WebListener
public class AppInitListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppInitListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            UserDAO userDAO = new UserDAO();
            if (userDAO.count() > 0) {
                return;
            }
            userDAO.insert(newUser("admin", "admin123", "Quản trị hệ thống", Role.ADMIN, null));
            int lecturerId = userDAO.insert(newUser("lecturer1", "123456", "Giảng viên Demo", Role.LECTURER, null));
            userDAO.insert(newUser("student1", "123456", "Sinh viên Demo", Role.STUDENT, "SE000001"));

            List<Integer> subjectIds = new ArrayList<>();
            for (Subject s : new SubjectDAO().findAll()) {
                subjectIds.add(s.getId());
            }
            userDAO.replaceLecturerSubjects(lecturerId, subjectIds);
            LOG.info("Đã tạo tài khoản demo: admin / lecturer1 / student1");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không tạo được tài khoản demo (đã chạy database/aives_schema.sql chưa?)", e);
        }
    }

    private User newUser(String username, String password, String fullName, Role role, String studentCode) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setFullName(fullName);
        u.setRole(role);
        u.setStudentCode(studentCode);
        u.setActive(true);
        return u;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
