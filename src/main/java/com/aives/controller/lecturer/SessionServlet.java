package com.aives.controller.lecturer;

import com.aives.dao.InterviewDAO;
import com.aives.dao.SubjectDAO;
import com.aives.model.InterviewSession;
import com.aives.model.Role;
import com.aives.model.User;
import com.aives.service.AccessService;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Feature 3 - Giảng viên xem / xoá các lượt phỏng vấn AI (transcript đầy đủ).
 * GET  ?action=list|view&id=
 * POST action=delete
 */
@WebServlet("/lecturer/sessions")
public class SessionServlet extends HttpServlet {

    private final InterviewDAO interviewDAO = new InterviewDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final AccessService access = new AccessService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        try {
            if ("view".equals(req.getParameter("action"))) {
                InterviewSession se = loadAuthorized(req, resp);
                if (se == null) {
                    return;
                }
                se.setTurns(interviewDAO.findTurns(se.getId()));
                req.setAttribute("se", se);
                req.getRequestDispatcher("/WEB-INF/views/lecturer/session-detail.jsp").forward(req, resp);
            } else {
                int subjectId = WebUtil.intParam(req, "subjectId", 0);
                req.setAttribute("subjects", user.getRole() == Role.ADMIN
                        ? subjectDAO.findAll() : subjectDAO.findByLecturer(user.getId()));
                List<InterviewSession> sessions = interviewDAO.search(null, access.lecturerScope(user),
                        subjectId == 0 ? null : subjectId);
                Map<String, Integer> stats = new HashMap<>();
                stats.put("TOTAL", sessions.size());
                for (InterviewSession s : sessions) {
                    stats.merge(s.getStatus(), 1, Integer::sum);
                }
                req.setAttribute("sessions", sessions);
                req.setAttribute("stats", stats);
                req.getRequestDispatcher("/WEB-INF/views/lecturer/sessions.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            InterviewSession se = loadAuthorized(req, resp);
            if (se == null) {
                return;
            }
            interviewDAO.deleteSession(se.getId());
            WebUtil.flash(req, "success", "Đã xoá lượt phỏng vấn.");
            resp.sendRedirect(req.getContextPath() + "/lecturer/sessions");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private InterviewSession loadAuthorized(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, IOException {
        InterviewSession se = interviewDAO.findSession(WebUtil.intParam(req, "id", -1));
        if (se == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        if (!access.canManageSubject(WebUtil.currentUser(req), se.getSubjectId())) {
            WebUtil.denyAccess(req, resp);
            return null;
        }
        return se;
    }
}
