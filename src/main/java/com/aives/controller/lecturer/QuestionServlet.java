package com.aives.controller.lecturer;

import com.aives.dao.QuestionDAO;
import com.aives.dao.RubricDAO;
import com.aives.dao.SubjectDAO;
import com.aives.model.BloomLevel;
import com.aives.model.Question;
import com.aives.model.QuestionSource;
import com.aives.model.QuestionStatus;
import com.aives.model.Role;
import com.aives.model.User;
import com.aives.service.AccessService;
import com.aives.service.QuestionGenerator;
import com.aives.service.TemplateQuestionGenerator;
import com.aives.util.WebUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Feature 1 - CRUD ngân hàng câu hỏi.
 * GET  ?action=list|create|edit|generate&id=
 * POST action=save|delete|status|generate
 */
@WebServlet("/lecturer/questions")
public class QuestionServlet extends HttpServlet {

    private static final String LIST_VIEW = "/WEB-INF/views/lecturer/questions.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/lecturer/question-form.jsp";
    private static final String GENERATE_VIEW = "/WEB-INF/views/lecturer/question-generate.jsp";

    private final QuestionDAO questionDAO = new QuestionDAO();
    private final RubricDAO rubricDAO = new RubricDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final AccessService access = new AccessService();
    private final QuestionGenerator generator = new TemplateQuestionGenerator();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        User user = WebUtil.currentUser(req);
        try {
            req.setAttribute("subjects", manageableSubjects(user));
            req.setAttribute("blooms", BloomLevel.values());
            req.setAttribute("statuses", QuestionStatus.values());
            if ("create".equals(action)) {
                Question q = new Question();
                q.setBloomLevel(BloomLevel.UNDERSTAND);
                q.setSubjectId(WebUtil.intParam(req, "subjectId", 0));
                req.setAttribute("q", q);
                req.getRequestDispatcher(FORM_VIEW).forward(req, resp);
            } else if ("edit".equals(action)) {
                Question q = loadAuthorized(req, resp, WebUtil.intParam(req, "id", -1));
                if (q == null) {
                    return;
                }
                q.setRubrics(rubricDAO.findByQuestion(q.getId()));
                req.setAttribute("q", q);
                req.getRequestDispatcher(FORM_VIEW).forward(req, resp);
            } else if ("generate".equals(action)) {
                req.getRequestDispatcher(GENERATE_VIEW).forward(req, resp);
            } else {
                list(req, resp, user);
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
                Question q = loadAuthorized(req, resp, WebUtil.intParam(req, "id", -1));
                if (q != null) {
                    questionDAO.delete(q.getId());
                    WebUtil.flash(req, "success", "Đã xoá câu hỏi.");
                    resp.sendRedirect(req.getContextPath() + "/lecturer/questions");
                }
            } else if ("status".equals(action)) {
                changeStatus(req, resp);
            } else if ("generate".equals(action)) {
                generate(req, resp);
            } else {
                save(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        int subjectId = WebUtil.intParam(req, "subjectId", 0);
        String status = req.getParameter("status");
        String bloom = req.getParameter("bloom");
        req.setAttribute("questions", questionDAO.search(
                access.lecturerScope(user),
                subjectId == 0 ? null : subjectId,
                WebUtil.isBlank(status) ? null : QuestionStatus.valueOf(status),
                WebUtil.isBlank(bloom) ? null : BloomLevel.valueOf(bloom),
                WebUtil.trim(req.getParameter("q"))));
        req.setAttribute("stats", questionDAO.countByStatus(access.lecturerScope(user)));
        req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        User user = WebUtil.currentUser(req);
        int id = WebUtil.intParam(req, "id", 0);
        Question q = new Question();
        q.setId(id);
        q.setSubjectId(WebUtil.intParam(req, "subjectId", 0));
        q.setTopic(WebUtil.trim(req.getParameter("topic")));
        q.setContent(WebUtil.trim(req.getParameter("content")));
        q.setReferenceAnswer(WebUtil.trim(req.getParameter("referenceAnswer")));
        String bloom = req.getParameter("bloomLevel");
        q.setBloomLevel(WebUtil.isBlank(bloom) ? null : BloomLevel.valueOf(bloom));

        if (id != 0 && loadAuthorized(req, resp, id) == null) {
            return;
        }
        String error = null;
        if (!access.canManageSubject(user, q.getSubjectId())) {
            error = "Bạn không được phân công môn học này.";
        } else if (WebUtil.isBlank(q.getContent())) {
            error = "Nội dung câu hỏi không được để trống.";
        } else if (q.getBloomLevel() == null) {
            error = "Chưa chọn mức độ Bloom.";
        }
        if (error != null) {
            if (id != 0) {
                q.setRubrics(rubricDAO.findByQuestion(id));
                q.setStatus(questionDAO.findById(id).getStatus());
            }
            req.setAttribute("error", error);
            req.setAttribute("q", q);
            req.setAttribute("subjects", manageableSubjects(user));
            req.setAttribute("blooms", BloomLevel.values());
            req.getRequestDispatcher(FORM_VIEW).forward(req, resp);
            return;
        }

        if (id == 0) {
            q.setStatus(QuestionStatus.DRAFT);
            q.setSource(QuestionSource.MANUAL);
            q.setCreatedBy(user.getId());
            id = questionDAO.insert(q);
            WebUtil.flash(req, "success", "Đã tạo câu hỏi. Thêm rubric rồi bấm Duyệt để đưa vào ngân hàng chính thức.");
        } else {
            questionDAO.update(q);
            WebUtil.flash(req, "success", "Đã cập nhật câu hỏi.");
        }
        resp.sendRedirect(req.getContextPath() + "/lecturer/questions?action=edit&id=" + id);
    }

    /** Duyệt (APPROVED) / loại bỏ (REJECTED) / đưa về nháp (DRAFT). */
    private void changeStatus(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Question q = loadAuthorized(req, resp, WebUtil.intParam(req, "id", -1));
        if (q == null) {
            return;
        }
        QuestionStatus target = QuestionStatus.valueOf(req.getParameter("to"));
        if (target == QuestionStatus.APPROVED && rubricDAO.findByQuestion(q.getId()).isEmpty()) {
            WebUtil.flash(req, "error", "Cần ít nhất một tiêu chí rubric trước khi duyệt câu hỏi.");
        } else {
            questionDAO.updateStatus(q.getId(), target,
                    target == QuestionStatus.DRAFT ? null : WebUtil.currentUser(req).getId());
            WebUtil.flash(req, "success", "Đã chuyển trạng thái: " + target.getLabel());
        }
        String back = req.getParameter("back");
        resp.sendRedirect(req.getContextPath() + ("list".equals(back)
                ? "/lecturer/questions?status=" + QuestionStatus.PENDING_REVIEW
                : "/lecturer/questions?action=edit&id=" + q.getId()));
    }

    /** Sinh câu hỏi bằng "AI" (stub) -> trạng thái Chờ duyệt. */
    private void generate(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User user = WebUtil.currentUser(req);
        int subjectId = WebUtil.intParam(req, "subjectId", 0);
        String topic = WebUtil.trim(req.getParameter("topic"));
        String[] levels = req.getParameterValues("levels");
        if (!access.canManageSubject(user, subjectId) || WebUtil.isBlank(topic) || levels == null) {
            WebUtil.flash(req, "error", "Chọn môn, nhập chủ đề và ít nhất một mức Bloom.");
            resp.sendRedirect(req.getContextPath() + "/lecturer/questions?action=generate");
            return;
        }
        List<BloomLevel> bloomLevels = new ArrayList<>();
        for (String l : levels) {
            bloomLevels.add(BloomLevel.valueOf(l));
        }
        List<Question> generated = generator.generate(subjectId, topic, bloomLevels);
        for (Question q : generated) {
            q.setCreatedBy(user.getId());
            questionDAO.insert(q);
        }
        WebUtil.flash(req, "success", "AI đã sinh " + generated.size() + " câu hỏi, đang chờ bạn duyệt.");
        resp.sendRedirect(req.getContextPath() + "/lecturer/questions?status=" + QuestionStatus.PENDING_REVIEW);
    }

    /** Lấy câu hỏi và kiểm tra quyền; tự gửi 404/403 và trả null nếu không hợp lệ. */
    private Question loadAuthorized(HttpServletRequest req, HttpServletResponse resp, int id)
            throws SQLException, IOException {
        Question q = questionDAO.findById(id);
        if (q == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        if (!access.canManageSubject(WebUtil.currentUser(req), q.getSubjectId())) {
            WebUtil.denyAccess(req, resp);
            return null;
        }
        return q;
    }

    private Object manageableSubjects(User user) throws SQLException {
        return user.getRole() == Role.ADMIN ? subjectDAO.findAll() : subjectDAO.findByLecturer(user.getId());
    }
}
