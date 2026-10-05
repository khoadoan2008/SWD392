package com.aives.controller.admin;

import com.aives.dao.SubjectDAO;
import com.aives.dao.UserDAO;
import com.aives.model.Role;
import com.aives.model.User;
import com.aives.util.PasswordUtil;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/admin/users")
public class UserServlet extends HttpServlet {

    private static final String LIST_VIEW = "/WEB-INF/views/admin/users.jsp";

    private final UserDAO userDAO = new UserDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                User u = new User();
                u.setActive(true);
                u.setRole(Role.STUDENT);
                showForm(req, resp, u, Collections.<Integer>emptyList());
            } else if ("edit".equals(action)) {
                User u = userDAO.findById(WebUtil.intParam(req, "id", -1));
                if (u == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                showForm(req, resp, u, userDAO.findSubjectIdsOfLecturer(u.getId()));
            } else {
                loadList(req);
                req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("delete".equals(action)) {
                delete(req, resp);
            } else {
                save(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        int id = WebUtil.intParam(req, "id", 0);
        boolean isNew = id == 0;
        User u = new User();
        u.setId(id);
        u.setUsername(WebUtil.trim(req.getParameter("username")));
        u.setFullName(WebUtil.trim(req.getParameter("fullName")));
        u.setEmail(WebUtil.trim(req.getParameter("email")));
        u.setStudentCode(WebUtil.trim(req.getParameter("studentCode")));
        u.setActive(req.getParameter("active") != null);
        String roleParam = req.getParameter("role");
        u.setRole(WebUtil.isBlank(roleParam) ? null : Role.valueOf(roleParam));
        String password = req.getParameter("password");

        List<Integer> subjectIds = new ArrayList<>();
        String[] subjects = req.getParameterValues("subjectIds");
        if (subjects != null) {
            for (String s : subjects) {
                subjectIds.add(Integer.parseInt(s));
            }
        }

        String error = null;
        User current = WebUtil.currentUser(req);
        if (isNew && WebUtil.isBlank(u.getUsername())) {
            error = "Tên đăng nhập không được để trống.";
        } else if (isNew && !u.getUsername().matches("[A-Za-z0-9._]{3,50}")) {
            error = "Tên đăng nhập 3-50 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới.";
        } else if (isNew && userDAO.findByUsername(u.getUsername()) != null) {
            error = "Tên đăng nhập đã tồn tại.";
        } else if (WebUtil.isBlank(u.getFullName())) {
            error = "Họ tên không được để trống.";
        } else if (u.getRole() == null) {
            error = "Chưa chọn vai trò.";
        } else if (u.getRole() == Role.STUDENT && WebUtil.isBlank(u.getStudentCode())) {
            error = "Sinh viên phải có MSSV.";
        } else if (isNew && (password == null || password.length() < 6)) {
            error = "Mật khẩu tối thiểu 6 ký tự.";
        } else if (!isNew && !WebUtil.isBlank(password) && password.length() < 6) {
            error = "Mật khẩu mới tối thiểu 6 ký tự.";
        } else if (!isNew && id == current.getId() && (!u.isActive() || u.getRole() != Role.ADMIN)) {
            error = "Không thể tự khoá hoặc tự hạ quyền tài khoản đang đăng nhập.";
        }
        if (error != null) {
            if (!isNew) {
                u.setUsername(userDAO.findById(id).getUsername());
            }
            req.setAttribute("error", error);
            showForm(req, resp, u, subjectIds);
            return;
        }

        if (u.getRole() != Role.STUDENT) {
            u.setStudentCode(null);
        }
        u.setPasswordHash(WebUtil.isBlank(password) ? null : PasswordUtil.hash(password));
        if (isNew) {
            id = userDAO.insert(u);
        } else {
            userDAO.update(u);
        }
        userDAO.replaceLecturerSubjects(id,
                u.getRole() == Role.LECTURER ? subjectIds : Collections.<Integer>emptyList());

        WebUtil.flash(req, "success", isNew ? "Đã tạo tài khoản." : "Đã cập nhật tài khoản.");
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = WebUtil.intParam(req, "id", -1);
        if (id == WebUtil.currentUser(req).getId()) {
            WebUtil.flash(req, "error", "Không thể xoá tài khoản đang đăng nhập.");
        } else {
            try {
                userDAO.delete(id);
                WebUtil.flash(req, "success", "Đã xoá tài khoản.");
            } catch (SQLException e) {
                WebUtil.flash(req, "error", "Tài khoản đã có dữ liệu liên quan, hãy khoá (bỏ tick Hoạt động) thay vì xoá.");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, User u, List<Integer> subjectIds)
            throws SQLException, ServletException, IOException {
        req.setAttribute("u", u);
        req.setAttribute("subjects", subjectDAO.findAll());
        req.setAttribute("assignedSubjectIds", subjectIds);
        loadList(req); 
        req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
    }

    private void loadList(HttpServletRequest req) throws SQLException {
        String roleParam = req.getParameter("filterRole");
        Role role = WebUtil.isBlank(roleParam) ? null : Role.valueOf(roleParam);
        req.setAttribute("users", userDAO.search(role, WebUtil.trim(req.getParameter("q"))));
        req.setAttribute("stats", userDAO.countByRole());
        req.setAttribute("roles", Role.values());
    }
}
