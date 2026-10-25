package com.aives.controller;

import com.aives.model.entity.*;
import com.aives.model.enums.*;
import com.aives.repository.*;
import com.aives.security.*;
import com.aives.service.*;
import com.aives.service.generation.*;
import com.fasterxml.jackson.databind.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class QuestionImportTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired QuestionRepository questions;
    @Autowired RubricRepository rubrics;
    @Autowired Pbkdf2CompatiblePasswordEncoder encoder;
    @Autowired JwtTokenProvider tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired QuestionImportService imports;
    @MockBean MaterialQuestionGenerator generator;
    User lecturer, other, student, admin;
    Subject subject;

    @BeforeEach void setup() {
        jdbc.update("delete from rubrics"); questions.deleteAll();
        jdbc.update("delete from lecturer_subjects"); users.deleteAll(); subjects.deleteAll();
        lecturer = user("importer", Role.LECTURER); other = user("other", Role.LECTURER);
        student = user("student", Role.STUDENT); admin = user("admin", Role.ADMIN);
        subject = new Subject(); subject.setCode("PRJ301"); subject.setName("Java Web");
        subject = subjects.saveAndFlush(subject);
        lecturer.getAssignedSubjects().add(subject); lecturer = users.saveAndFlush(lecturer);
    }
    User user(String name, Role role) {
        User u = new User(); u.setUsername(name); u.setFullName(name); u.setRole(role);
        u.setActive(true); u.setPasswordHash(encoder.encode("Demo@123")); return users.saveAndFlush(u);
    }
    String bearer(User u) {
        var principal = new UserPrincipal(u);
        return "Bearer " + tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    String[] row(String content) { return new String[]{"PRJ301", content, "UNDERSTAND", "MEDIUM", "Gợi ý kiểm thử"}; }
    byte[] excel(String[]... rows) throws Exception {
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Cau hoi"); var header = sheet.createRow(0);
            for (int c = 0; c < 5; c++) header.createCell(c).setCellValue(QuestionImportService.HEADERS[c]);
            for (int r = 0; r < rows.length; r++) {
                var line = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) line.createCell(c).setCellValue(rows[r][c]);
            }
            wb.write(out); return out.toByteArray();
        }
    }
    MockMultipartFile file(byte[] bytes) { return new MockMultipartFile("file", "questions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes); }
    MockMultipartHttpServletRequestBuilder request(User u, byte[] bytes) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/lecturer/questions/import").file(file(bytes)).header("Authorization", bearer(u));
    }
    String preview(byte[] bytes) throws Exception {
        var result = mvc.perform(request(lecturer, bytes)).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("data").path("fileHash").asText();
    }
    void addRubric() { rubrics.saveAndFlush(Rubric.builder().subject(subject).criterion("Đúng kiến thức").maxScore(BigDecimal.TEN).sortOrder(1).build()); }

    @Test void templateHasFiveHeadersAndDropdowns() throws Exception {
        byte[] bytes = mvc.perform(get("/api/lecturer/questions/import/template").header("Authorization", bearer(lecturer)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", "attachment; filename=Question_Template.xlsx"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertEquals(1, wb.getNumberOfSheets());
            for (int c = 0; c < 5; c++) assertEquals(QuestionImportService.HEADERS[c], wb.getSheetAt(0).getRow(0).getCell(c).getStringCellValue());
            assertEquals(2, wb.getSheetAt(0).getDataValidations().size());
        }
    }
    @Test void previewDoesNotPersist() throws Exception {
        mvc.perform(request(lecturer, excel(row("Câu hỏi mới"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.validRows").value(1)).andExpect(jsonPath("$.data.importedCount").value(0));
        assertEquals(0, questions.count());
    }
    @Test void confirmPersistsMetadataAndSourceAndDefaultPending() throws Exception {
        byte[] bytes = excel(row("Câu hỏi nhập Excel")); String hash = preview(bytes);
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.importedCount").value(1));
        var q = questions.findAll().getFirst();
        assertEquals(QuestionSource.IMPORT, q.getSource()); assertEquals(QuestionStatus.PENDING_REVIEW, q.getStatus());
        assertEquals(DifficultyLevel.MEDIUM, q.getDifficulty()); assertEquals("Gợi ý kiểm thử", q.getNotes());
        assertNull(q.getReferenceAnswer());
        mvc.perform(get("/api/lecturer/questions/" + q.getId()).header("Authorization", bearer(lecturer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.data.notes").value("Gợi ý kiểm thử"));
    }
    @Test void invalidRowsAreDetailedAndEntireBatchIsBlocked() throws Exception {
        var bad = row(""); bad[2] = "CREATE"; bad[3] = "IMPOSSIBLE";
        byte[] bytes = excel(row("Dòng hợp lệ"), bad); String hash = preview(bytes);
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.data.invalidRows").value(1))
                .andExpect(jsonPath("$.data.rows[1].rowNumber").value(3))
                .andExpect(jsonPath("$.data.rows[1].errors.length()").value(3));
        assertEquals(0, questions.count());
    }
    @Test void duplicatesInsideFileAreRejectedIgnoringCaseAndWhitespace() throws Exception {
        mvc.perform(request(lecturer, excel(row("Hello   World"), row(" hello world "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.data.invalidRows").value(1));
    }
    @Test void confirmChecksDatabaseAgainAndReplayDoesNotInsertTwice() throws Exception {
        byte[] bytes = excel(row("Trùng dữ liệu")); String hash = preview(bytes);
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash)).andExpect(status().isOk());
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.data.invalidRows").value(1));
        assertEquals(1, questions.count());
    }
    @Test void unassignedLecturerCannotImportSubject() throws Exception {
        mvc.perform(request(other, excel(row("Không được phép"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        assertEquals(0, questions.count());
    }
    @Test void assignmentRevokedBetweenPreviewAndConfirmIsRejected() throws Exception {
        byte[] bytes = excel(row("Quyền bị thu hồi")); String hash = preview(bytes);
        jdbc.update("delete from lecturer_subjects");
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash)).andExpect(status().isUnprocessableEntity());
        assertEquals(0, questions.count());
    }
    @Test void studentAndAnonymousCannotUseImportOrTemplate() throws Exception {
        mvc.perform(request(student, excel(row("Không có quyền")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/lecturer/questions/import/template").header("Authorization", bearer(student))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/lecturer/questions/import").file(file(excel(row("Ẩn danh"))))).andExpect(status().isForbidden());
    }
    @Test void adminCanImportWithoutAssignment() throws Exception {
        byte[] bytes = excel(row("Admin nhập"));
        var p = imports.process(admin.getId(), file(bytes), false, QuestionStatus.PENDING_REVIEW, null);
        assertEquals(1, imports.process(admin.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, p.fileHash()).importedCount());
    }
    @Test void approvedRequiresSubjectRubricAndSetsReviewer() throws Exception {
        byte[] bytes = excel(row("Duyệt chính thức")); String hash = preview(bytes);
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash).param("status", "APPROVED"))
                .andExpect(status().isUnprocessableEntity());
        addRubric();
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash).param("status", "APPROVED"))
                .andExpect(status().isOk());
        assertEquals(QuestionStatus.APPROVED, questions.findAll().getFirst().getStatus());
        assertEquals(lecturer.getId().intValue(), jdbc.queryForObject("select reviewed_by from questions", Integer.class));
    }
    @Test void modifiedFileOrMissingPreviewHashCannotConfirm() throws Exception {
        byte[] original = excel(row("Tệp gốc")); String hash = preview(original);
        mvc.perform(request(lecturer, excel(row("Tệp khác"))).param("confirm", "true").param("previewHash", hash)).andExpect(status().isBadRequest());
        mvc.perform(request(lecturer, original).param("confirm", "true")).andExpect(status().isBadRequest());
        assertEquals(0, questions.count());
    }
    @Test void rejectsWrongExtensionCorruptAndEmptyFile() throws Exception {
        mvc.perform(multipart("/api/lecturer/questions/import").file(new MockMultipartFile("file", "a.csv", "text/csv", "hello".getBytes())).header("Authorization", bearer(lecturer))).andExpect(status().isBadRequest());
        mvc.perform(request(lecturer, "not an xlsx".getBytes())).andExpect(status().isBadRequest());
        mvc.perform(request(lecturer, new byte[0])).andExpect(status().isBadRequest());
    }
    @Test void rejectsOversizedFileAndWrongHeaders() throws Exception {
        mvc.perform(request(lecturer, new byte[10 * 1024 * 1024 + 1])).andExpect(status().isBadRequest());
        byte[] bytes;
        try (var wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excel(row("A")))); var out = new ByteArrayOutputStream()) {
            wb.getSheetAt(0).getRow(0).getCell(0).setCellValue("Sai tiêu đề"); wb.write(out); bytes = out.toByteArray();
        }
        mvc.perform(request(lecturer, bytes)).andExpect(status().isBadRequest());
    }
    @Test void rejectsFormulaAndExtraDataColumn() throws Exception {
        byte[] bytes;
        try (var wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excel(row("A")))); var out = new ByteArrayOutputStream()) {
            wb.getSheetAt(0).getRow(1).getCell(1).setCellFormula("1+1"); wb.write(out); bytes = out.toByteArray();
        }
        mvc.perform(request(lecturer, bytes)).andExpect(jsonPath("$.success").value(false));
        mvc.perform(request(lecturer, excel(new String[]{"PRJ301", "A", "REMEMBER", "EASY", "", "Extra"})))
                .andExpect(jsonPath("$.success").value(false));
    }
    @Test void rejectsEmptyWorkbookTooManyRowsAndTooLongNotes() throws Exception {
        mvc.perform(request(lecturer, excel())).andExpect(status().isBadRequest());
        String[][] rows = new String[1001][]; for (int n = 0; n < rows.length; n++) rows[n] = row("Câu " + n);
        mvc.perform(request(lecturer, excel(rows))).andExpect(status().isBadRequest());
        var bad = row("Có ghi chú dài"); bad[4] = "x".repeat(2001);
        mvc.perform(request(lecturer, excel(bad))).andExpect(jsonPath("$.data.invalidRows").value(1));
    }
    @Test void importingTwentyQuestionsWithinTwoSecondsAfterWarmup() throws Exception {
        String[][] rows = new String[20][]; for (int n = 0; n < 20; n++) rows[n] = row("Câu kiểm thử hiệu năng " + n);
        byte[] bytes = excel(rows);
        var p = imports.process(lecturer.getId(), file(bytes), false, QuestionStatus.PENDING_REVIEW, null);
        long start = System.nanoTime();
        var result = imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, p.fileHash());
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertEquals(20, result.importedCount()); assertEquals(20, questions.count());
        System.out.println("IMPORT_20_WARM_DB_MS=" + elapsed);
        assertTrue(elapsed < 2000, "Warm service + transaction time: " + elapsed + " ms");
    }
    @Test void concurrentImportsDoNotDuplicate() throws Exception {
        byte[] bytes = excel(row("Nhập đồng thời"));
        String hash = imports.process(lecturer.getId(), file(bytes), false, QuestionStatus.PENDING_REVIEW, null).fileHash();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, hash));
            var b = executor.submit(() -> imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, hash));
            assertEquals(1, a.get(15, TimeUnit.SECONDS).importedCount() + b.get(15, TimeUnit.SECONDS).importedCount());
        }
        assertEquals(1, questions.count());
    }
    @Test void directManualApprovalAlsoRequiresRubric() throws Exception {
        String body = "{\"subjectId\":" + subject.getId() + ",\"content\":\"Manual\",\"bloomLevel\":\"UNDERSTAND\",\"status\":\"APPROVED\"}";
        mvc.perform(post("/api/lecturer/questions").header("Authorization", bearer(lecturer)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(0, questions.count());
    }
    @Test void rejectedStatusAndInactiveSubjectAreNotImportable() throws Exception {
        byte[] bytes = excel(row("Tạm dừng"));
        mvc.perform(request(lecturer, bytes).param("status", "REJECTED")).andExpect(status().isBadRequest());
        subject.setActive(false); subjects.saveAndFlush(subject);
        mvc.perform(request(lecturer, bytes)).andExpect(jsonPath("$.data.invalidRows").value(1));
    }

    @Test void databaseFailureRollsBackEntireBatch() throws Exception {
        byte[] bytes = excel(row("Dòng đầu"), row("FAIL_IMPORT_CHECK")); String hash = preview(bytes);
        jdbc.execute("alter table questions add constraint import_test_reject check(content <> 'FAIL_IMPORT_CHECK')");
        try {
            assertThrows(RuntimeException.class, () -> imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, hash));
            assertEquals(0, questions.count());
        } finally { jdbc.execute("alter table questions drop constraint import_test_reject"); }
    }

    @Test void mixedSubjectsValidateEachRowAndCannotPartiallySave() throws Exception {
        Subject extra = new Subject(); extra.setCode("DBI202"); extra.setName("Database"); subjects.saveAndFlush(extra);
        var second = row("Môn khác"); second[0] = "DBI202";
        byte[] bytes = excel(row("Môn được phép"), second); String hash = preview(bytes);
        mvc.perform(request(lecturer, bytes).param("confirm", "true").param("previewHash", hash)).andExpect(status().isUnprocessableEntity());
        assertEquals(0, questions.count());
    }

    @Test void disabledActorCannotConfirm() throws Exception {
        byte[] bytes = excel(row("Actor bị khóa")); String hash = preview(bytes);
        lecturer.setActive(false); users.saveAndFlush(lecturer);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, hash));
        assertEquals(0, questions.count());
    }

    @Test void oldReviewClientDoesNotEraseImportedMetadata() throws Exception {
        var imported = row("Câu hỏi khó"); imported[3] = "HARD";
        byte[] bytes = excel(imported); String hash = preview(bytes);
        imports.process(lecturer.getId(), file(bytes), true, QuestionStatus.PENDING_REVIEW, hash);
        var q = questions.findAll().getFirst();
        String body = "{\"subjectId\":" + subject.getId() + ",\"content\":\"Đã sửa\",\"bloomLevel\":\"UNDERSTAND\"}";
        mvc.perform(put("/api/lecturer/questions/" + q.getId()).header("Authorization", bearer(lecturer)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.difficulty").value("HARD"))
                .andExpect(jsonPath("$.data.notes").value("Gợi ý kiểm thử"));
    }
}
