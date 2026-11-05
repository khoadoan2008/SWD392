package com.aives.feature;

import com.aives.model.entity.SystemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Feature7SystemAdminTest extends FeatureTestBase {

    String newUser(String username, String role, String password, Integer subjectId) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"fullName\":\"Nguyễn Văn A\",\"email\":\"a@fpt.edu.vn\",\"role\":\""
                + role + "\",\"active\":true" + (subjectId == null ? "" : ",\"subjectIds\":[" + subjectId + "]") + "}";
    }
    String login(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test void loginReturnsTokenAndRole() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content(login("lecturer1", "Demo@123")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.user.role").value("LECTURER"));
    }
    @Test void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content(login("lecturer1", "sai-mat-khau")))
            .andExpect(status().isUnauthorized());
    }
    @Test void lockedAccountCannotLogin() throws Exception {
        lecturer.setActive(false); users.saveAndFlush(lecturer);
        mvc.perform(post("/api/auth/login").contentType("application/json").content(login("lecturer1", "Demo@123")))
            .andExpect(status().is4xxClientError()).andExpect(jsonPath("$.data.token").doesNotExist());
    }
    @Test void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }

    @Test void adminCreatesLecturerWithAssignedSubject() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer(admin)).contentType("application/json")
                .content(newUser("gv_moi", "LECTURER", "Secret@1", subject.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("LECTURER"));
        var created = users.findByUsername("gv_moi").orElseThrow();
        assertEquals(1, created.getAssignedSubjects().size());
        assertNotEquals("Secret@1", created.getPasswordHash());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(login("gv_moi", "Secret@1")))
            .andExpect(status().isOk());
    }
    @Test void duplicateUsernameIsRejected() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer(admin)).contentType("application/json")
                .content(newUser("lecturer1", "LECTURER", "Secret@1", null)))
            .andExpect(status().isBadRequest());
    }
    @Test void shortPasswordAndMissingRoleAreRejected() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", bearer(admin)).contentType("application/json")
                .content(newUser("u_short", "STUDENT", "123", null))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"username\":\"u_norole\",\"password\":\"Secret@1\",\"fullName\":\"X\"}")).andExpect(status().isBadRequest());
        assertTrue(users.findByUsername("u_short").isEmpty()); assertTrue(users.findByUsername("u_norole").isEmpty());
    }
    @Test void adminSearchesUsersByRoleAndKeyword() throws Exception {
        mvc.perform(get("/api/admin/users").param("role", "STUDENT").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/admin/users").param("q", "lecturer2").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }
    @Test void userStatsCountByRole() throws Exception {
        mvc.perform(get("/api/admin/users/stats").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.TOTAL").value(5))
            .andExpect(jsonPath("$.data.LECTURER").value(2)).andExpect(jsonPath("$.data.STUDENT").value(2));
    }
    @Test void adminReassignsLecturerSubjects() throws Exception {
        String body = "{\"fullName\":\"User lecturer1\",\"role\":\"LECTURER\",\"active\":true,\"subjectIds\":[" + otherSubject.getId() + "]}";
        mvc.perform(put("/api/admin/users/" + lecturer.getId()).header("Authorization", bearer(admin)).contentType("application/json").content(body))
            .andExpect(status().isOk());
        var ids = users.findById(lecturer.getId()).orElseThrow().getAssignedSubjects().stream().map(s -> s.getId()).toList();
        assertEquals(java.util.List.of(otherSubject.getId()), ids);
    }
    @Test void adminLocksUserAndResetsPassword() throws Exception {
        String body = "{\"fullName\":\"User student1\",\"role\":\"STUDENT\",\"active\":false,\"newPassword\":\"NewPass@1\"}";
        mvc.perform(put("/api/admin/users/" + student.getId()).header("Authorization", bearer(admin)).contentType("application/json").content(body))
            .andExpect(status().isOk());
        var u = users.findById(student.getId()).orElseThrow();
        assertFalse(u.isActive()); assertTrue(encoder.matches("NewPass@1", u.getPasswordHash()));
    }
    @Test void adminDeletesUser() throws Exception {
        mvc.perform(delete("/api/admin/users/" + otherStudent.getId()).header("Authorization", bearer(admin))).andExpect(status().isOk());
        assertTrue(users.findById(otherStudent.getId()).isEmpty());
    }
    @Test void adminCannotDeleteOwnAccount() throws Exception {
        mvc.perform(delete("/api/admin/users/" + admin.getId()).header("Authorization", bearer(admin))).andExpect(status().isBadRequest());
        assertTrue(users.findById(admin.getId()).isPresent());
    }
    @Test void nonAdminCannotManageUsers() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(lecturer))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").header("Authorization", bearer(student)).contentType("application/json")
                .content(newUser("hacker", "ADMIN", "Secret@1", null))).andExpect(status().isForbidden());
        assertTrue(users.findByUsername("hacker").isEmpty());
    }

    @Test void adminCreatesSubjectWithUppercaseCode() throws Exception {
        mvc.perform(post("/api/admin/subjects").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"code\":\"prn231\",\"name\":\"ASP.NET\",\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("PRN231"));
    }
    @Test void duplicateSubjectCodeIsRejected() throws Exception {
        mvc.perform(post("/api/admin/subjects").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"code\":\"swd392\",\"name\":\"Trùng\",\"active\":true}"))
            .andExpect(status().isBadRequest());
    }
    @Test void adminUpdatesAndDeletesSubject() throws Exception {
        var s = subject("TMP101", "Tạm");
        mvc.perform(put("/api/admin/subjects/" + s.getId()).header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"code\":\"TMP101\",\"name\":\"Đã sửa\",\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Đã sửa"));
        mvc.perform(delete("/api/admin/subjects/" + s.getId()).header("Authorization", bearer(admin))).andExpect(status().isOk());
        assertTrue(subjects.findById(s.getId()).isEmpty());
    }
    @Test void deletingSubjectInUseReturnsClearError() throws Exception {
        approvedQuestion(subject, "Câu hỏi đang dùng");
        mvc.perform(delete("/api/admin/subjects/" + subject.getId()).header("Authorization", bearer(admin)))
            .andExpect(status().is4xxClientError());
    }

    @Test void configsHaveDefaults() throws Exception {
        mvc.perform(get("/api/admin/configs").header("Authorization", bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data['speech.language']").value("vi-VN"))
            .andExpect(jsonPath("$.data['interview.answer_time']").value("120"))
            .andExpect(jsonPath("$.data['interview.max_followups']").value("2"))
            .andExpect(jsonPath("$.data['interview.main_questions']").value("3"));
    }
    @Test void adminSwitchesSpeechLanguageToEnglish() throws Exception {
        mvc.perform(put("/api/admin/configs").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"speech.language\":\"en-US\",\"interview.max_followups\":\"1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data['speech.language']").value("en-US"))
            .andExpect(jsonPath("$.data['interview.max_followups']").value("1"));
    }
    @Test void invalidConfigValuesAreRejected() throws Exception {
        mvc.perform(put("/api/admin/configs").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"interview.answer_time\":\"-5\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/configs").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"speech.language\":\"klingon\"}")).andExpect(status().isBadRequest());
    }
    @Test void unknownConfigKeyIsRejected() throws Exception {
        mvc.perform(put("/api/admin/configs").header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"random.key\":\"x\"}")).andExpect(status().isBadRequest());
    }
    @Test void nonAdminCannotChangeConfig() throws Exception {
        mvc.perform(put("/api/admin/configs").header("Authorization", bearer(lecturer)).contentType("application/json")
                .content("{\"" + SystemConfig.SPEECH_LANGUAGE + "\":\"en-US\"}")).andExpect(status().isForbidden());
    }
}
