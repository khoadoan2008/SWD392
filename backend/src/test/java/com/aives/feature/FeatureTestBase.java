package com.aives.feature;

import com.aives.model.entity.*;
import com.aives.model.enums.*;
import com.aives.repository.*;
import com.aives.security.*;
import com.aives.service.AiGradingTrigger;
import com.aives.service.MaterialQuestionGenerator;
import com.aives.service.impl.GeminiAdaptiveFollowUpGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
abstract class FeatureTestBase {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired QuestionRepository questions;
    @Autowired RubricRepository rubrics;
    @Autowired RoleRepository roles;
    @Autowired Pbkdf2CompatiblePasswordEncoder encoder;
    @Autowired JwtTokenProvider tokens;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockBean MaterialQuestionGenerator generator;
    @MockBean GeminiAdaptiveFollowUpGenerator followUps;
    @MockBean AiGradingTrigger gradingTrigger;

    User admin, lecturer, otherLecturer, student, otherStudent;
    Subject subject, otherSubject;

    @BeforeEach void resetData() {
        for (String t : new String[]{"session_rubric_scores", "interview_turns", "interview_sessions", "rubrics",
                "questions", "lecturer_subjects", "system_configs"}) jdbc.update("delete from " + t);
        users.deleteAll(); subjects.deleteAll();
        subject = subject("SWD392", "Software Architecture");
        otherSubject = subject("DBI202", "Database");
        admin = user("admin1", Role.ADMIN);
        lecturer = user("lecturer1", Role.LECTURER);
        otherLecturer = user("lecturer2", Role.LECTURER);
        student = user("student1", Role.STUDENT);
        otherStudent = user("student2", Role.STUDENT);
        lecturer.getAssignedSubjects().add(subject); lecturer = users.saveAndFlush(lecturer);
        otherLecturer.getAssignedSubjects().add(otherSubject); otherLecturer = users.saveAndFlush(otherLecturer);
        student.getAssignedSubjects().add(subject); student = users.saveAndFlush(student);
        otherStudent.getAssignedSubjects().add(subject); otherStudent = users.saveAndFlush(otherStudent);
    }

    Subject subject(String code, String name) {
        Subject s = new Subject(); s.setCode(code); s.setName(name); s.setActive(true);
        return subjects.saveAndFlush(s);
    }

    User user(String name, Role role) {
        User u = new User(); u.setUsername(name); u.setFullName("User " + name);
        u.setRole(roles.findByNameIgnoreCase(role.getName()).orElseGet(() -> roles.save(new Role(role.getName()))));
        u.setActive(true); u.setPasswordHash(encoder.encode("Demo@123"));
        return users.saveAndFlush(u);
    }

    String bearer(User u) {
        var principal = new UserPrincipal(u);
        return "Bearer " + tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    Question approvedQuestion(Subject s, String content) {
        return questions.saveAndFlush(Question.builder().subject(s).content(content).bloomLevel(BloomLevel.UNDERSTAND)
                .status(QuestionStatus.APPROVED).createdBy(lecturer).build());
    }

    Rubric rubric(Subject s, String criterion, String keywords, double max) {
        return rubrics.saveAndFlush(Rubric.builder().subject(s).criterion(criterion).keywords(keywords)
                .maxScore(BigDecimal.valueOf(max)).sortOrder(1).build());
    }

    void config(String key, String value) {
        jdbc.update("insert into system_configs(config_key, config_value) values (?, ?)", key, value);
    }
}
