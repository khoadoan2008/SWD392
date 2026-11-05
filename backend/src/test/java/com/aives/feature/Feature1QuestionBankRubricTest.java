package com.aives.feature;

import com.aives.model.enums.QuestionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Feature1QuestionBankRubricTest extends FeatureTestBase {

    String questionJson(int subjectId, String content) {
        return "{\"subjectId\":" + subjectId + ",\"topic\":\"Layered\",\"content\":\"" + content
                + "\",\"referenceAnswer\":\"Controller, service, repository\",\"bloomLevel\":\"UNDERSTAND\"}";
    }
    String rubricJson(String criterion, String max) {
        return "{\"criterion\":\"" + criterion + "\",\"keywords\":\"controller,service\",\"maxScore\":" + max + ",\"sortOrder\":1}";
    }

    @Test void lecturerCreatesAndListsRubricForAssignedSubject() throws Exception {
        mvc.perform(post("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(lecturer))
                .contentType("application/json").content(rubricJson("Kiến trúc phân tầng", "5")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.criterion").value("Kiến trúc phân tầng"));
        mvc.perform(get("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].keywords").value("controller,service"));
    }
    @Test void rubricRejectsBlankCriterionAndNonPositiveScore() throws Exception {
        mvc.perform(post("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(lecturer))
                .contentType("application/json").content(rubricJson("", "5"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(lecturer))
                .contentType("application/json").content(rubricJson("Tiêu chí", "0"))).andExpect(status().isBadRequest());
        assertEquals(0, rubrics.count());
    }
    @Test void lecturerUpdatesAndDeletesRubric() throws Exception {
        var r = rubric(subject, "Cũ", "a", 4);
        mvc.perform(put("/api/lecturer/rubrics/" + r.getId()).param("subjectId", subject.getId().toString())
                .header("Authorization", bearer(lecturer)).contentType("application/json").content(rubricJson("Mới", "6")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.criterion").value("Mới"))
            .andExpect(jsonPath("$.data.maxScore").value(6));
        mvc.perform(delete("/api/lecturer/rubrics/" + r.getId()).header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk());
        assertEquals(0, rubrics.count());
    }
    @Test void unassignedLecturerCannotChangeRubricOfOtherSubject() throws Exception {
        mvc.perform(post("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(otherLecturer))
                .contentType("application/json").content(rubricJson("Không được phép", "5")))
            .andExpect(status().isForbidden());
        assertEquals(0, rubrics.count());
    }
    @Test void studentCannotAccessRubrics() throws Exception {
        mvc.perform(get("/api/lecturer/rubrics/subject/" + subject.getId()).header("Authorization", bearer(student)))
            .andExpect(status().isForbidden());
    }

    @Test void manualQuestionStartsAsDraftWithBloomLevel() throws Exception {
        mvc.perform(post("/api/lecturer/questions").header("Authorization", bearer(lecturer))
                .contentType("application/json").content(questionJson(subject.getId(), "Trình bày kiến trúc phân tầng")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.bloomLevel").value("UNDERSTAND"))
            .andExpect(jsonPath("$.data.source").value("MANUAL"));
        assertEquals(QuestionStatus.DRAFT, questions.findAll().getFirst().getStatus());
    }
    @Test void questionRequiresContentAndBloomLevel() throws Exception {
        mvc.perform(post("/api/lecturer/questions").header("Authorization", bearer(lecturer)).contentType("application/json")
                .content("{\"subjectId\":" + subject.getId() + ",\"content\":\"\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(0, questions.count());
    }
    @Test void lecturerCannotCreateQuestionForUnassignedSubject() throws Exception {
        mvc.perform(post("/api/lecturer/questions").header("Authorization", bearer(lecturer))
                .contentType("application/json").content(questionJson(otherSubject.getId(), "Sai môn")))
            .andExpect(status().isForbidden());
        assertEquals(0, questions.count());
    }
    @Test void lecturerRejectsQuestion() throws Exception {
        var q = approvedQuestion(subject, "Câu hỏi cần loại");
        mvc.perform(patch("/api/lecturer/questions/" + q.getId() + "/status").param("to", "REJECTED")
                .header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
    @Test void listFiltersByStatusAndSubject() throws Exception {
        approvedQuestion(subject, "Đã duyệt 1");
        approvedQuestion(otherSubject, "Môn khác");
        mvc.perform(get("/api/lecturer/questions").param("status", "APPROVED").header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].content").value("Đã duyệt 1"));
    }
    @Test void lecturerDeletesOwnSubjectQuestion() throws Exception {
        var q = approvedQuestion(subject, "Xoá tôi");
        mvc.perform(delete("/api/lecturer/questions/" + q.getId()).header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk());
        assertEquals(0, questions.count());
    }
    @Test void unassignedLecturerCannotDeleteQuestion() throws Exception {
        var q = approvedQuestion(subject, "Không được xoá");
        mvc.perform(delete("/api/lecturer/questions/" + q.getId()).header("Authorization", bearer(otherLecturer)))
            .andExpect(status().isForbidden());
        assertEquals(1, questions.count());
    }
}
