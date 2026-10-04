package com.aives.controller.lecturer;

import com.aives.dao.QuestionDAO;
import com.aives.dao.RubricDAO;
import com.aives.model.Question;
import com.aives.model.Rubric;
import com.aives.service.AccessService;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Feature 1 - CRUD rubric, thao tác ngay trong trang sửa câu hỏi.
 * POST action=save (id=0 -> thêm mới) | delete ; luôn kèm questionId
 */
@WebServlet("/lecturer/rubrics")
public class RubricServlet extends HttpServlet {

    private final RubricDAO rubricDAO = new RubricDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AccessService access = new AccessService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int questionId = WebUtil.intParam(req, "questionId", -1);
        try {
            Question q = questionDAO.findById(questionId);
            if (q == null || !access.canManageSubject(WebUtil.currentUser(req), q.getSubjectId())) {
                WebUtil.denyAccess(req, resp);
                return;
            }
            int id = WebUtil.intParam(req, "id", 0);
            if (id != 0) {
                Rubric existing = rubricDAO.findById(id);
                if (existing == null || existing.getQuestionId() != questionId) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            if ("delete".equals(req.getParameter("action"))) {
                rubricDAO.delete(id);
                WebUtil.flash(req, "success", "Đã xoá tiêu chí.");
            } else {
                Rubric r = new Rubric();
                r.setId(id);
                r.setQuestionId(questionId);
                r.setCriterion(WebUtil.trim(req.getParameter("criterion")));
                r.setKeywords(WebUtil.trim(req.getParameter("keywords")));
                r.setSortOrder(WebUtil.intParam(req, "sortOrder", 0));
                BigDecimal score = parseScore(req.getParameter("maxScore"));
                if (WebUtil.isBlank(r.getCriterion()) || score == null) {
                    WebUtil.flash(req, "error", "Tiêu chí không được trống và điểm tối đa phải là số > 0.");
                } else {
                    r.setMaxScore(score);
                    if (id == 0) {
                        rubricDAO.insert(r);
                    } else {
                        rubricDAO.update(r);
                    }
                    WebUtil.flash(req, "success", "Đã lưu tiêu chí.");
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/lecturer/questions?action=edit&id=" + questionId + "#rubrics");
    }

    private BigDecimal parseScore(String s) {
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.signum() > 0 && v.compareTo(new BigDecimal("100")) <= 0 ? v : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
