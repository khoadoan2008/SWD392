package com.aives.controller;

import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.Role;
import com.aives.repository.QuestionRepository;
import com.aives.repository.SubjectRepository;
import com.aives.repository.UserRepository;
import com.aives.security.*;
import com.aives.service.*;
import com.aives.service.generation.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class QuestionWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired QuestionRepository questions;
    @Autowired Pbkdf2CompatiblePasswordEncoder encoder;
    @Autowired JwtTokenProvider tokens;
    @Autowired DraftPersistenceService persistence;
    @Autowired JdbcTemplate jdbc;
    @MockBean MaterialQuestionGenerator generator;
    private User lecturer, other, student, admin;
    private Subject subject;

    @BeforeEach void setup() throws Exception {
        questions.deleteAll();
        // Reset only the isolated in-memory test database, never the user's databases.
        jdbc.update("delete from lecturer_subjects");
        users.deleteAll(); subjects.deleteAll(); reset(generator);
        lecturer=user("lecturer1",Role.LECTURER); other=user("lecturer2",Role.LECTURER);
        student=user("student1",Role.STUDENT); admin=user("admin",Role.ADMIN);
        subject=new Subject(); subject.setCode("DBI202"); subject.setName("Cơ sở dữ liệu");
        subject=subjects.saveAndFlush(subject);
        lecturer.getAssignedSubjects().add(subject); lecturer=users.saveAndFlush(lecturer);
        when(generator.generate(any(GenerationRequest.class))).thenAnswer(invocation ->
            new GeneratedQuestionParser().parse(GenerationFixtures.valid(),invocation.getArgument(0)));
    }
    private User user(String username,Role role) {
        User user=new User(); user.setUsername(username); user.setFullName("Người dùng "+username);
        user.setPasswordHash(encoder.encode("Demo@123")); user.setRole(role); user.setActive(true);
        return users.saveAndFlush(user);
    }
    private String bearer(User user) {
        var principal=new UserPrincipal(user);
        return "Bearer "+tokens.generateToken(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    private MockHttpServletRequestBuilder generation(User user,int subjectId,String consent,String key) {
        return multipart("/api/lecturer/questions/generate")
            .file(new MockMultipartFile("materialFile","course.txt","text/plain",GenerationFixtures.MATERIAL.getBytes(StandardCharsets.UTF_8)))
            .param("subjectId",Integer.toString(subjectId)).param("topic","TCP and UDP").param("levels","ANALYZE")
            .param("allowAiTransfer",consent).header("Authorization",bearer(user)).header("Idempotency-Key",key);
    }
    @Test void loginStillWorksOnLeaderCode() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"lecturer1\",\"password\":\"Demo@123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isString());
    }
    @Test void bothLoopbackFrontendOriginsCanPreflight() throws Exception {
        for (String origin:List.of("http://localhost:5173","http://127.0.0.1:5173"))
            mvc.perform(options("/api/lecturer/questions/generate").header("Origin",origin)
                    .header("Access-Control-Request-Method","POST")
                    .header("Access-Control-Request-Headers","idempotency-key,authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin));
    }
    @Test void unrelatedOriginIsRejected() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","https://unrelated.example")
                .header("Access-Control-Request-Method","POST"))
            .andExpect(status().isForbidden());
    }
    @Test void lecturerSeesOnlyAssignedGenerationSubjects() throws Exception {
        Subject extra=new Subject(); extra.setCode("PRJ301"); extra.setName("Java"); subjects.saveAndFlush(extra);
        mvc.perform(get("/api/lecturer/questions/generation-subjects").header("Authorization",bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Cơ sở dữ liệu"));
        mvc.perform(get("/api/lecturer/questions/generation-subjects").header("Authorization",bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
    }
    @Test void studentCannotListOrGenerate() throws Exception {
        mvc.perform(get("/api/lecturer/questions/generation-subjects").header("Authorization",bearer(student))).andExpect(status().isForbidden());
        mvc.perform(generation(student,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        verifyNoInteractions(generator);
    }
    @Test void unassignedLecturerDeniedBeforeProvider() throws Exception {
        mvc.perform(generation(other,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        verifyNoInteractions(generator); assertEquals(0,questions.count());
    }
    @Test void missingConsentDeniedBeforeProvider() throws Exception {
        mvc.perform(generation(lecturer,subject.getId(),"false",UUID.randomUUID().toString())).andExpect(status().isBadRequest());
        verifyNoInteractions(generator);
    }
    @Test void generateThenReviewUsingLeadersExistingStatusRoute() throws Exception {
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].source").value("AI"))
            .andExpect(jsonPath("$.data[0].status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.data[0].rubrics.length()").value(2));
        int id=questions.findAll().get(0).getId();
        mvc.perform(patch("/api/lecturer/questions/"+id+"/status").param("to","APPROVED").header("Authorization",bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        mvc.perform(get("/api/lecturer/questions").param("status","APPROVED").header("Authorization",bearer(lecturer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }
    @Test void replayDoesNotCallProviderTwice() throws Exception {
        String key=UUID.randomUUID().toString();
        mvc.perform(generation(lecturer,subject.getId(),"true",key)).andExpect(status().isOk());
        mvc.perform(generation(lecturer,subject.getId(),"true",key)).andExpect(status().isConflict());
        verify(generator,times(1)).generate(any(GenerationRequest.class));
    }
    @Test void approvalRejectsAiRubricWhoseTotalWasEdited() throws Exception {
        var request=new GenerationRequest(subject.getId(),subject.getName(),"TCP",List.of(BloomLevel.ANALYZE),GenerationFixtures.MATERIAL);
        var generated=new GeneratedQuestionParser().parse(GenerationFixtures.valid(),request);
        int id=persistence.saveAll(lecturer.getId(),request,generated).get(0).id();
        jdbc.update("update rubrics set max_score=1 where question_id=?",id);
        mvc.perform(patch("/api/lecturer/questions/"+id+"/status").param("to","APPROVED").header("Authorization",bearer(lecturer)))
            .andExpect(status().isBadRequest());
        assertEquals(com.aives.model.enums.QuestionStatus.PENDING_REVIEW,questions.findById(id).orElseThrow().getStatus());
    }
    @Test void assignmentRevokedDuringAiCallPreventsSaving() throws Exception {
        when(generator.generate(any(GenerationRequest.class))).thenAnswer(invocation -> {
            jdbc.update("delete from lecturer_subjects where lecturer_id=?",lecturer.getId());
            return new GeneratedQuestionParser().parse(GenerationFixtures.valid(),invocation.getArgument(0));
        });
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        assertEquals(0,questions.count());
    }
    @Test void invalidRequestKeyRejectedBeforeProvider() throws Exception {
        mvc.perform(generation(lecturer,subject.getId(),"true","not-a-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(generator);
    }
    @Test void accountDisabledDuringProviderCallPreventsSaving() throws Exception {
        when(generator.generate(any(GenerationRequest.class))).thenAnswer(invocation -> {
            jdbc.update("update users set active=false where id=?",lecturer.getId());
            return new GeneratedQuestionParser().parse(GenerationFixtures.valid(),invocation.getArgument(0));
        });
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        assertEquals(0,questions.count());
    }
    @Test void subjectDisabledDuringProviderCallPreventsSaving() throws Exception {
        when(generator.generate(any(GenerationRequest.class))).thenAnswer(invocation -> {
            jdbc.update("update subjects set active=false where id=?",subject.getId());
            return new GeneratedQuestionParser().parse(GenerationFixtures.valid(),invocation.getArgument(0));
        });
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isBadRequest());
        assertEquals(0,questions.count());
    }
    @Test void providerFailureHasNoTemplateFallbackOrSavedRows() throws Exception {
        when(generator.generate(any(GenerationRequest.class))).thenThrow(new GenerationException("Gemini unavailable"));
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString())).andExpect(status().isBadGateway());
        assertEquals(0,questions.count());
    }
    @Test void malformedBloomIsBadRequest() throws Exception {
        mvc.perform(generation(lecturer,subject.getId(),"true",UUID.randomUUID().toString()).param("levels","INVALID"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(generator);
    }
    @Test void emptyFileIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/lecturer/questions/generate").file(new MockMultipartFile("materialFile","empty.txt","text/plain",new byte[0]))
                .param("subjectId",subject.getId().toString()).param("topic","TCP").param("levels","ANALYZE")
                .param("allowAiTransfer","true").header("Idempotency-Key",UUID.randomUUID().toString()).header("Authorization",bearer(lecturer)))
            .andExpect(status().isBadRequest()); verifyNoInteractions(generator);
    }
    @Test void databaseFailureRollsBackQuestionAndRubrics() throws Exception {
        var request=new GenerationRequest(subject.getId(),subject.getName(),"TCP",List.of(BloomLevel.ANALYZE),GenerationFixtures.MATERIAL);
        List<Question> generated=new GeneratedQuestionParser().parse(GenerationFixtures.valid(),request);
        generated.get(0).getRubrics().get(1).setCriterion(null);
        assertThrows(RuntimeException.class,()->persistence.saveAll(lecturer.getId(),request,generated));
        assertEquals(0,questions.count()); assertEquals(0,jdbc.queryForObject("select count(*) from rubrics",Integer.class));
    }
    @Test void legacyJsonTemplateRouteRemainsSeparate() throws Exception {
        mvc.perform(post("/api/lecturer/questions/generate").header("Authorization",bearer(lecturer))
                .contentType("application/json").content("{\"subjectId\":"+subject.getId()+",\"topic\":\"TCP\",\"levels\":[\"ANALYZE\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("không gọi AI")));
        verifyNoInteractions(generator);
    }
}
