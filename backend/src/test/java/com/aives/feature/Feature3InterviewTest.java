package com.aives.feature;

import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.SystemConfig;
import com.aives.model.enums.SessionStatus;
import com.aives.repository.InterviewSessionRepository;
import com.aives.repository.InterviewTurnRepository;
import com.aives.service.impl.RuleBasedFollowUpGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Feature3InterviewTest extends FeatureTestBase {
    @Autowired InterviewSessionRepository sessions;
    @Autowired InterviewTurnRepository turns;
    @Autowired RuleBasedFollowUpGenerator ruleBased;

    static final String LONG_ANSWER = "Kiến trúc phân tầng gồm controller nhận request, service xử lý nghiệp vụ và repository truy cập dữ liệu";

    int start(com.aives.model.entity.User u) throws Exception {
        var res = mvc.perform(post("/api/student/interviews/start").param("subjectId", subject.getId().toString())
                .header("Authorization", bearer(u))).andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data").path("id").asInt();
    }
    JsonNode current(int sessionId, com.aives.model.entity.User u) throws Exception {
        var res = mvc.perform(get("/api/student/interviews/" + sessionId).header("Authorization", bearer(u)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");
    }
    void submit(int sessionId, int turnId, String transcript) throws Exception {
        mvc.perform(post("/api/student/interviews/" + sessionId + "/submit").header("Authorization", bearer(student))
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("turnId", turnId, "transcript", transcript))))
            .andExpect(status().isOk());
    }
    void useRuleBasedFollowUps() {
        when(followUps.generate(any(), any(), any(), any())).thenAnswer(i -> ruleBased.generate(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));
    }

    @Test void startCreatesSessionWithConfiguredMainQuestionsAndLimits() throws Exception {
        for (int n = 1; n <= 4; n++) approvedQuestion(subject, "Câu hỏi " + n);
        config(SystemConfig.MAIN_QUESTIONS, "2"); config(SystemConfig.ANSWER_TIME, "90"); config(SystemConfig.MAX_FOLLOWUPS, "1");
        mvc.perform(post("/api/student/interviews/start").param("subjectId", subject.getId().toString()).header("Authorization", bearer(student)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.data.turns.length()").value(2))
            .andExpect(jsonPath("$.data.answerTimeLimitSec").value(90))
            .andExpect(jsonPath("$.data.maxFollowups").value(1));
    }
    @Test void onlyApprovedQuestionsAreAsked() throws Exception {
        approvedQuestion(subject, "Đã duyệt");
        var draft = approvedQuestion(subject, "Bản nháp"); draft.setStatus(com.aives.model.enums.QuestionStatus.DRAFT); questions.saveAndFlush(draft);
        int id = start(student);
        assertEquals(List.of("Đã duyệt"), turns.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(id).stream().map(InterviewTurn::getQuestionText).toList());
    }
    @Test void startReturnsExistingInProgressSession() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int first = start(student), second = start(student);
        assertEquals(first, second); assertEquals(1, sessions.count());
    }
    @Test void differentStudentsGetIndependentSessions() throws Exception {
        approvedQuestion(subject, "Câu 1");
        assertNotEquals(start(student), start(otherStudent));
    }
    @Test void studentNotAssignedToSubjectCannotStart() throws Exception {
        approvedQuestion(otherSubject, "Câu môn khác");
        mvc.perform(post("/api/student/interviews/start").param("subjectId", otherSubject.getId().toString()).header("Authorization", bearer(student)))
            .andExpect(status().isBadRequest());
        assertEquals(0, sessions.count());
    }
    @Test void subjectWithoutApprovedQuestionsCannotStart() throws Exception {
        mvc.perform(post("/api/student/interviews/start").param("subjectId", subject.getId().toString()).header("Authorization", bearer(student)))
            .andExpect(status().is4xxClientError());
        assertEquals(0, sessions.count());
    }
    @Test void lecturerCannotUseStudentInterviewApi() throws Exception {
        mvc.perform(post("/api/student/interviews/start").param("subjectId", subject.getId().toString()).header("Authorization", bearer(lecturer)))
            .andExpect(status().isForbidden());
    }

    @Test void currentTurnReturnsQuestionRemainingTimeAndSpeechLanguage() throws Exception {
        approvedQuestion(subject, "Câu 1"); config(SystemConfig.ANSWER_TIME, "60"); config(SystemConfig.SPEECH_LANGUAGE, "en-US");
        JsonNode data = current(start(student), student);
        assertEquals("Câu 1", data.path("currentTurn").path("questionText").asText());
        assertTrue(data.path("remainingSec").asLong() <= 60 && data.path("remainingSec").asLong() >= 58);
        assertEquals("en-US", data.path("speechLang").asText());
        assertFalse(data.path("currentTurn").path("askedAt").isNull());
    }
    @Test void defaultSpeechLanguageIsVietnamese() throws Exception {
        approvedQuestion(subject, "Câu 1");
        assertEquals("vi-VN", current(start(student), student).path("speechLang").asText());
    }
    @Test void otherStudentCannotReadSession() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int id = start(student);
        mvc.perform(get("/api/student/interviews/" + id).header("Authorization", bearer(otherStudent)))
            .andExpect(status().isForbidden());
    }

    @Test void transcriptIsSavedWithResponseTime() throws Exception {
        approvedQuestion(subject, "Câu 1"); config(SystemConfig.MAX_FOLLOWUPS, "0");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        submit(id, turnId, LONG_ANSWER);
        var t = turns.findById(turnId).orElseThrow();
        assertEquals(LONG_ANSWER, t.getTranscript()); assertNotNull(t.getAnsweredAt()); assertNotNull(t.getResponseTimeSec());
    }
    @Test void shortAnswerTriggersClarifyingFollowUp() throws Exception {
        useRuleBasedFollowUps(); approvedQuestion(subject, "Trình bày kiến trúc phân tầng");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        submit(id, turnId, "Có ba tầng");
        JsonNode data = current(id, student);
        assertEquals("FOLLOW_UP", data.path("currentTurn").path("turnType").asText());
        assertTrue(data.path("currentTurn").path("questionText").asText().contains("giải thích chi tiết hơn"));
        assertEquals("Có ba tầng", data.path("prevTurn").path("transcript").asText());
    }
    @Test void followUpUsesConfiguredEnglishLanguage() throws Exception {
        useRuleBasedFollowUps(); approvedQuestion(subject, "Explain layered architecture"); config(SystemConfig.SPEECH_LANGUAGE, "en-US");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        submit(id, turnId, "Three layers");
        JsonNode data = current(id, student);
        assertEquals("en-US", data.path("speechLang").asText());
        assertTrue(data.path("currentTurn").path("questionText").asText().startsWith("Your answer is quite short"));
    }
    @Test void missingRubricKeywordTriggersProbingFollowUp() throws Exception {
        useRuleBasedFollowUps(); approvedQuestion(subject, "Trình bày REST API");
        rubric(subject, "HTTP methods", "GET,POST,PUT,DELETE", 5);
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        submit(id, turnId, "REST API là kiểu kiến trúc dùng URL để định danh tài nguyên và trả về dữ liệu JSON cho client");
        assertTrue(current(id, student).path("currentTurn").path("questionText").asText().contains("HTTP methods"));
    }
    @Test void completeAnswerMovesToNextMainQuestion() throws Exception {
        useRuleBasedFollowUps(); approvedQuestion(subject, "Câu A"); approvedQuestion(subject, "Câu B");
        int id = start(student); JsonNode first = current(id, student).path("currentTurn");
        submit(id, first.path("id").asInt(), LONG_ANSWER);
        JsonNode next = current(id, student).path("currentTurn");
        assertEquals("MAIN", next.path("turnType").asText()); assertEquals(2, next.path("mainIndex").asInt());
    }
    @Test void followUpsNeverExceedConfiguredMaximum() throws Exception {
        when(followUps.generate(any(), any(), any(), any())).thenAnswer(i -> "Hỏi xoáy lần " + System.nanoTime());
        approvedQuestion(subject, "Câu 1"); config(SystemConfig.MAX_FOLLOWUPS, "2");
        int id = start(student);
        for (int n = 0; n < 5; n++) {
            JsonNode turn = current(id, student).path("currentTurn");
            if (turn.isMissingNode() || turn.isNull()) break;
            submit(id, turn.path("id").asInt(), "Trả lời " + n);
        }
        long followUpCount = turns.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(id).stream().filter(InterviewTurn::isFollowUp).count();
        assertEquals(2, followUpCount);
        assertEquals(SessionStatus.COMPLETED, sessions.findById(id).orElseThrow().getStatus());
    }
    @Test void answeringLastTurnCompletesSessionAndTriggersAiGrading() throws Exception {
        approvedQuestion(subject, "Câu 1"); config(SystemConfig.MAX_FOLLOWUPS, "0");
        int id = start(student);
        submit(id, current(id, student).path("currentTurn").path("id").asInt(), LONG_ANSWER);
        var s = sessions.findById(id).orElseThrow();
        assertEquals(SessionStatus.COMPLETED, s.getStatus()); assertNotNull(s.getEndedAt());
        verify(gradingTrigger).gradeAfterCommit(id);
    }
    @Test void resubmittingSameTurnDoesNotOverwriteAnswer() throws Exception {
        approvedQuestion(subject, "Câu 1"); approvedQuestion(subject, "Câu 2"); config(SystemConfig.MAX_FOLLOWUPS, "0");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        submit(id, turnId, "Lần đầu"); submit(id, turnId, "Lần hai");
        assertEquals("Lần đầu", turns.findById(turnId).orElseThrow().getTranscript());
    }
    @Test void answerAfterTimeLimitIsNotRecorded() throws Exception {
        useRuleBasedFollowUps();
        approvedQuestion(subject, "Câu 1"); approvedQuestion(subject, "Câu 2"); config(SystemConfig.ANSWER_TIME, "30");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        jdbc.update("update interview_turns set asked_at = dateadd('SECOND', -300, asked_at) where id = ?", turnId);
        mvc.perform(post("/api/student/interviews/" + id + "/submit").header("Authorization", bearer(student))
                .contentType("application/json").content("{\"turnId\":" + turnId + ",\"transcript\":\"Nộp muộn 5 phút\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("hết thời gian")));
        var late = turns.findById(turnId).orElseThrow();
        assertNull(late.getTranscript()); assertNotNull(late.getAnsweredAt());
        JsonNode next = current(id, student).path("currentTurn");
        assertEquals("MAIN", next.path("turnType").asText()); assertEquals(2, next.path("mainIndex").asInt());
    }
    @Test void answerWithinGracePeriodIsRecorded() throws Exception {
        approvedQuestion(subject, "Câu 1"); approvedQuestion(subject, "Câu 2"); config(SystemConfig.MAX_FOLLOWUPS, "0"); config(SystemConfig.ANSWER_TIME, "30");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        jdbc.update("update interview_turns set asked_at = dateadd('SECOND', -35, asked_at) where id = ?", turnId);
        submit(id, turnId, LONG_ANSWER);
        assertEquals(LONG_ANSWER, turns.findById(turnId).orElseThrow().getTranscript());
    }
    @Test void otherStudentCannotSubmitAnswer() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        mvc.perform(post("/api/student/interviews/" + id + "/submit").header("Authorization", bearer(otherStudent))
                .contentType("application/json").content("{\"turnId\":" + turnId + ",\"transcript\":\"Gian lận\"}"))
            .andExpect(status().isForbidden());
        assertNull(turns.findById(turnId).orElseThrow().getTranscript());
    }
    @Test void submitWithoutTurnIdIsBadRequest() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int id = start(student);
        mvc.perform(post("/api/student/interviews/" + id + "/submit").header("Authorization", bearer(student))
                .contentType("application/json").content("{\"transcript\":\"abc\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void abortEndsSessionAndBlocksFurtherAnswers() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int id = start(student); int turnId = current(id, student).path("currentTurn").path("id").asInt();
        mvc.perform(post("/api/student/interviews/" + id + "/abort").header("Authorization", bearer(student))).andExpect(status().isOk());
        submit(id, turnId, "Sau khi huỷ");
        assertEquals(SessionStatus.ABORTED, sessions.findById(id).orElseThrow().getStatus());
        assertNull(turns.findById(turnId).orElseThrow().getTranscript());
    }
    @Test void lecturerOfOtherSubjectCannotViewSessionDetail() throws Exception {
        approvedQuestion(subject, "Câu 1");
        int id = start(student);
        mvc.perform(get("/api/lecturer/sessions/" + id).header("Authorization", bearer(lecturer))).andExpect(status().isOk());
        mvc.perform(get("/api/lecturer/sessions/" + id).header("Authorization", bearer(otherLecturer))).andExpect(status().isForbidden());
    }
    @Test void sessionSubjectFilterIncludesInactiveAssignedSubjects() throws Exception {
        subject.setActive(false); subjects.saveAndFlush(subject);
        mvc.perform(get("/api/lecturer/sessions/subjects").header("Authorization", bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].code").value("SWD392"));
        mvc.perform(get("/api/lecturer/sessions/subjects").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/lecturer/sessions/subjects").header("Authorization", bearer(student)))
            .andExpect(status().isForbidden());
    }
    @Test void homeShowsAssignedSubjectsAndHistory() throws Exception {
        approvedQuestion(subject, "Câu 1"); start(student);
        mvc.perform(get("/api/student/interviews/home").header("Authorization", bearer(student)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subjects.length()").value(1))
            .andExpect(jsonPath("$.data.history.length()").value(1));
    }

    @Test void ruleBasedFollowUpStopsWhenAnswerCoversAllKeywords() {
        var turn = InterviewTurn.builder().questionText("Câu hỏi").transcript(LONG_ANSWER).build();
        assertNull(ruleBased.generate("Câu hỏi", List.of(rubric(subject, "Tầng", "controller,service", 5)), List.of(turn)));
    }
    @Test void ruleBasedFollowUpDoesNotRepeatSameProbe() {
        var main = InterviewTurn.builder().questionText("Câu hỏi").transcript("ngắn").build();
        String first = ruleBased.generate("Câu hỏi", List.of(), List.of(main));
        var follow = InterviewTurn.builder().questionText(first).transcript("vẫn ngắn").build();
        assertNull(ruleBased.generate("Câu hỏi", List.of(), List.of(main, follow)));
    }
}
