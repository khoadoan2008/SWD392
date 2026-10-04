package com.aives.controller.admin;

import com.aives.dao.SubjectDAO;
import com.aives.model.Subject;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/admin/subjects")
public class SubjectServlet extends HttpServlet {

    private static final String LIST_VIEW = "/WEB-INF/views/admin/subjects.jsp";

    private final SubjectDAO subjectDAO = new SubjectDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                Subject s = new Subject();
                s.setActive(true);
                render(req, resp, s);
            } else if ("edit".equals(action)) {
                Subject s = subjectDAO.findById(WebUtil.intParam(req, "id", -1));
                if (s == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                render(req, resp, s);
            } else {
                render(req, resp, null);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("delete".equals(req.getParameter("action"))) {
            try {
                subjectDAO.delete(WebUtil.intParam(req, "id", -1));
                WebUtil.flash(req, "success", "Đã xoá môn học.");
            } catch (SQLException e) {
                WebUtil.flash(req, "error", "Môn học đã có câu hỏi hoặc lượt thi, hãy ngừng kích hoạt thay vì xoá.");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/subjects");
            return;
        }

        Subject s = new Subject();
        s.setId(WebUtil.intParam(req, "id", 0));
        s.setCode(WebUtil.trim(req.getParameter("code")));
        s.setName(WebUtil.trim(req.getParameter("name")));
        s.setDescription(WebUtil.trim(req.getParameter("description")));
        s.setActive(req.getParameter("active") != null);

        String error = null;
        if (WebUtil.isBlank(s.getCode()) || s.getCode().length() > 20) {
            error = "Mã môn bắt buộc, tối đa 20 ký tự.";
        } else if (WebUtil.isBlank(s.getName())) {
            error = "Tên môn không được để trống.";
        }
        try {
            if (error == null) {
                s.setCode(s.getCode().toUpperCase());
                if (s.getId() == 0) {
                    subjectDAO.insert(s);
                } else {
                    subjectDAO.update(s);
                }
                WebUtil.flash(req, "success", "Đã lưu môn học.");
                resp.sendRedirect(req.getContextPath() + "/admin/subjects");
                return;
            }
        } catch (SQLException e) {
            error = "Mã môn đã tồn tại.";
        }
        req.setAttribute("error", error);
        try {
            render(req, resp, s);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

 
    private void render(HttpServletRequest req, HttpServletResponse resp, Subject s)
            throws SQLException, ServletException, IOException {
        req.setAttribute("s", s);
        req.setAttribute("subjects", subjectDAO.findAll());
        req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
    }
}
