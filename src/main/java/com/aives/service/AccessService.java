package com.aives.service;

import com.aives.dao.UserDAO;
import com.aives.model.Role;
import com.aives.model.User;
import java.sql.SQLException;

/** Kiểm tra giảng viên có được phân công môn học hay không (admin được toàn quyền). */
public class AccessService {

    private final UserDAO userDAO = new UserDAO();

    public boolean canManageSubject(User user, int subjectId) throws SQLException {
        return user.getRole() == Role.ADMIN
                || (user.getRole() == Role.LECTURER && userDAO.findSubjectIdsOfLecturer(user.getId()).contains(subjectId));
    }

    /** null = không giới hạn (admin); ngược lại là id giảng viên dùng để lọc theo môn được phân công. */
    public Integer lecturerScope(User user) {
        return user.getRole() == Role.ADMIN ? null : user.getId();
    }
}
