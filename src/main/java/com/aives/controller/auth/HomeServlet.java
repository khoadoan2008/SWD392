package com.aives.controller.auth;

import com.aives.model.User;
import com.aives.util.WebUtil;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Điều hướng về trang chính theo vai trò sau khi đăng nhập. */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = WebUtil.currentUser(req);
        String target;
        switch (user.getRole()) {
            case ADMIN:
                target = "/admin/users";
                break;
            case LECTURER:
                target = "/lecturer/questions";
                break;
            default:
                target = "/student/interview";
        }
        resp.sendRedirect(req.getContextPath() + target);
    }
}
