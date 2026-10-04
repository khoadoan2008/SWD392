package com.aives.controller.auth;

import com.aives.dao.UserDAO;
import com.aives.model.User;
import com.aives.util.PasswordUtil;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        boolean hasNotice = session != null && session.getAttribute("flashMessage") != null;
        // Đã đăng nhập và không có lời nhắc (VD: bị từ chối quyền) -> về trang chính
        if (WebUtil.currentUser(req) != null && !hasNotice) {
            resp.sendRedirect(req.getContextPath() + "/home");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = WebUtil.trim(req.getParameter("username"));
        String password = req.getParameter("password");
        try {
            User user = WebUtil.isBlank(username) ? null : userDAO.findByUsername(username);
            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                fail(req, resp, username, "Tên đăng nhập hoặc mật khẩu chưa chính xác. Bạn vui lòng kiểm tra lại nhé.");
                return;
            }
            if (!user.isActive()) {
                fail(req, resp, username,
                        "Tài khoản của bạn hiện đang tạm khoá. Vui lòng liên hệ quản trị viên để được hỗ trợ.");
                return;
            }
            user.setPasswordHash(null); // không giữ hash trong session
            HttpSession session = req.getSession(true);
            req.changeSessionId(); // chống session fixation (giữ nguyên các attribute)
            String target = (String) session.getAttribute(WebUtil.REDIRECT_AFTER_LOGIN);
            session.removeAttribute(WebUtil.REDIRECT_AFTER_LOGIN);
            session.setAttribute("user", user);
            // Chỉ chấp nhận đường dẫn nội bộ để tránh open redirect
            boolean safe = target != null && target.startsWith("/") && !target.startsWith("//");
            resp.sendRedirect(req.getContextPath() + (safe ? target : "/home"));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String username, String error)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.setAttribute("username", username);
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }
}
