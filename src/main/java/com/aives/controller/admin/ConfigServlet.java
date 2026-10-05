package com.aives.controller.admin;

import com.aives.dao.SystemConfigDAO;
import com.aives.model.SystemConfig;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/admin/configs")
public class ConfigServlet extends HttpServlet {

    private final SystemConfigDAO configDAO = new SystemConfigDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map<String, String> cfg = new HashMap<>();
            for (SystemConfig c : configDAO.findAll()) {
                cfg.put(c.getKey(), c.getValue());
            }
            req.setAttribute("cfg", cfg);
            req.getRequestDispatcher("/WEB-INF/views/admin/configs.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String lang = req.getParameter(SystemConfig.SPEECH_LANGUAGE);
        int answerTime = WebUtil.intParam(req, SystemConfig.ANSWER_TIME, -1);
        int maxFollowups = WebUtil.intParam(req, SystemConfig.MAX_FOLLOWUPS, -1);
        int mainQuestions = WebUtil.intParam(req, SystemConfig.MAIN_QUESTIONS, -1);

        if (!"vi-VN".equals(lang) && !"en-US".equals(lang)) {
            WebUtil.flash(req, "error", "Ngôn ngữ chỉ hỗ trợ vi-VN hoặc en-US.");
        } else if (answerTime < 15 || answerTime > 900) {
            WebUtil.flash(req, "error", "Thời gian trả lời phải từ 15 đến 900 giây.");
        } else if (maxFollowups < 0 || maxFollowups > 5) {
            WebUtil.flash(req, "error", "Số câu hỏi xoáy tối đa từ 0 đến 5.");
        } else if (mainQuestions < 1 || mainQuestions > 20) {
            WebUtil.flash(req, "error", "Số câu hỏi chính từ 1 đến 20.");
        } else {
            try {
                configDAO.update(SystemConfig.SPEECH_LANGUAGE, lang);
                configDAO.update(SystemConfig.ANSWER_TIME, String.valueOf(answerTime));
                configDAO.update(SystemConfig.MAX_FOLLOWUPS, String.valueOf(maxFollowups));
                configDAO.update(SystemConfig.MAIN_QUESTIONS, String.valueOf(mainQuestions));
                WebUtil.flash(req, "success", "Đã lưu cấu hình.");
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/admin/configs");
    }
}
