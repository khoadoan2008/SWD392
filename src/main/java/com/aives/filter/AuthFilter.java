package com.aives.filter;

import com.aives.model.Role;
import com.aives.model.User;
import com.aives.util.WebUtil;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Feature 7 - Bắt buộc đăng nhập và phân quyền theo tiền tố URL:
 * /admin/*    -> ADMIN
 * /lecturer/* -> LECTURER, ADMIN
 * /student/*  -> STUDENT
 * Không trả lỗi 403: chưa đăng nhập / không đủ quyền đều được đưa về trang đăng nhập kèm lời nhắc.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.equals("/login") || path.equals("/logout") || path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            // Mở trang chủ khi chưa đăng nhập là bình thường -> không cần lời nhắc
            WebUtil.redirectToLogin(req, resp, isEntryPage(path) ? null : WebUtil.MSG_LOGIN_REQUIRED);
            return;
        }

        if (!isAllowed(path, user.getRole())) {
            WebUtil.denyAccess(req, resp);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isEntryPage(String path) {
        return path.equals("/") || path.equals("/index.jsp") || path.equals("/home");
    }

    private boolean isAllowed(String path, Role role) {
        if (path.startsWith("/admin/")) {
            return role == Role.ADMIN;
        }
        if (path.startsWith("/lecturer/")) {
            return role == Role.LECTURER || role == Role.ADMIN;
        }
        if (path.startsWith("/student/")) {
            return role == Role.STUDENT;
        }
        return true;
    }

    @Override
    public void destroy() {
    }
}
