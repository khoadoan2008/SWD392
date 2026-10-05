package com.aives.util;

import com.aives.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class WebUtil {

    private WebUtil() {
    }

    public static User currentUser(HttpServletRequest req) {
        return req.getSession(false) == null ? null : (User) req.getSession(false).getAttribute("user");
    }

    public static int intParam(HttpServletRequest req, String name, int def) {
        String v = req.getParameter(name);
        if (v == null || v.trim().isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Lưu thông báo flash vào session, hiển thị một lần ở trang kế tiếp. */
    public static void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flashType", type);
        req.getSession().setAttribute("flashMessage", message);
    }

    public static final String MSG_LOGIN_REQUIRED =
            "Vui lòng đăng nhập để tiếp tục sử dụng hệ thống.";
    public static final String MSG_NO_PERMISSION =
            "Rất tiếc, tài khoản của bạn hiện chưa được cấp quyền truy cập trang này. "
            + "Bạn vui lòng đăng nhập bằng tài khoản phù hợp hoặc liên hệ quản trị viên nếu cần hỗ trợ.";

    /** Session attribute: trang người dùng định vào, mở lại sau khi đăng nhập thành công. */
    public static final String REDIRECT_AFTER_LOGIN = "redirectAfterLogin";

    
    public static void redirectToLogin(HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {
        if ("GET".equals(req.getMethod())) {
            String target = req.getRequestURI().substring(req.getContextPath().length());
            if (req.getQueryString() != null) {
                target += "?" + req.getQueryString();
            }
            req.getSession().setAttribute(REDIRECT_AFTER_LOGIN, target);
        }
        if (message != null) {
            flash(req, "warning", message);
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /** Dùng trong servlet khi người dùng đã đăng nhập nhưng không có quyền với dữ liệu cụ thể. */
    public static void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        redirectToLogin(req, resp, MSG_NO_PERMISSION);
    }
}
