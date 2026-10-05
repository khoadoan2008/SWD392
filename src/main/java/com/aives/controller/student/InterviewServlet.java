package com.aives.controller.student;

import com.aives.dao.InterviewDAO;
import com.aives.dao.SubjectDAO;
import com.aives.dao.SystemConfigDAO;
import com.aives.model.InterviewSession;
import com.aives.model.InterviewTurn;
import com.aives.model.SystemConfig;
import com.aives.model.User;
import com.aives.service.InterviewService;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/student/interview")
public class InterviewServlet extends HttpServlet {

    private final InterviewDAO interviewDAO = new InterviewDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();
    private final InterviewService interviewService = new InterviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        try {
            int id = WebUtil.intParam(req, "id", 0);
            if (id == 0) {
                req.setAttribute("subjects", subjectDAO.findActive());
                req.setAttribute("sessions", interviewDAO.search(user.getId(), null, null));
                req.getRequestDispatcher("/WEB-INF/views/student/interview-home.jsp").forward(req, resp);
                return;
            }
            InterviewSession se = loadOwn(req, resp, id);
            if (se == null) {
                return;
            }
            InterviewTurn current = InterviewSession.IN_PROGRESS.equals(se.getStatus())
                    ? interviewDAO.findCurrentTurn(se.getId()) : null;
            if (current != null) {
                interviewDAO.markAsked(current.getId());
                current = interviewDAO.findTurn(current.getId());
                long elapsed = (System.currentTimeMillis() - current.getAskedAt().getTime()) / 1000;
                req.setAttribute("remainingSec", Math.max(0, se.getAnswerTimeLimitSec() - elapsed));
                req.setAttribute("turn", current);
                req.setAttribute("speechLang", configDAO.get(SystemConfig.SPEECH_LANGUAGE, "vi-VN"));
            }
            se.setTurns(interviewDAO.findTurns(se.getId()));
            if (current != null && current.isFollowUp()) {
                InterviewTurn prev = null;
                for (InterviewTurn t : se.getTurns()) {
                    if (t.getMainIndex() == current.getMainIndex() && t.getAnsweredAt() != null) {
                        prev = t;
                    }
                }
                req.setAttribute("prevTurn", prev);
            }
            req.setAttribute("se", se);
            req.getRequestDispatcher("/WEB-INF/views/student/interview.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = req.getParameter("action");
        try {
            if ("start".equals(action)) {
                try {
                    int sessionId = interviewService.start(user.getId(), WebUtil.intParam(req, "subjectId", -1));
                    resp.sendRedirect(req.getContextPath() + "/student/interview?id=" + sessionId);
                } catch (IllegalStateException e) {
                    WebUtil.flash(req, "error", e.getMessage());
                    resp.sendRedirect(req.getContextPath() + "/student/interview");
                }
                return;
            }

            InterviewSession se = loadOwn(req, resp, WebUtil.intParam(req, "id", -1));
            if (se == null) {
                return;
            }
            if (InterviewSession.IN_PROGRESS.equals(se.getStatus())) {
                if ("abort".equals(action)) {
                    interviewDAO.finishSession(se.getId(), InterviewSession.ABORTED);
                } else {
                    interviewService.submitAnswer(se, WebUtil.intParam(req, "turnId", -1),
                            req.getParameter("transcript"));
                }
            }
            resp.sendRedirect(req.getContextPath() + "/student/interview?id=" + se.getId());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private InterviewSession loadOwn(HttpServletRequest req, HttpServletResponse resp, int id)
            throws SQLException, IOException {
        InterviewSession se = interviewDAO.findSession(id);
        if (se == null || se.getStudentId() != WebUtil.currentUser(req).getId()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return se;
    }
}
