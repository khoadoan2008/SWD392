package com.aives.service;

import com.aives.model.entity.*;
import com.aives.model.enums.TurnType;
import com.aives.repository.*;
import com.aives.service.generation.AiConfiguration;
import com.aives.service.generation.AiTransport;
import com.aives.service.generation.GenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiGradingServiceTest {

    private AiTransport transport;
    private RubricRepository rubricRepository;
    private SessionRubricScoreRepository sessionRubricScoreRepository;
    private InterviewSessionRepository sessionRepository;
    private InterviewTurnRepository turnRepository;
    private AiGradingService gradingService;

    private Subject subject;
    private InterviewSession session;
    private List<Rubric> rubrics;
    private List<InterviewTurn> turns;

    @BeforeEach
    void setUp() {
        transport = mock(AiTransport.class);
        rubricRepository = mock(RubricRepository.class);
        sessionRubricScoreRepository = mock(SessionRubricScoreRepository.class);
        sessionRepository = mock(InterviewSessionRepository.class);
        turnRepository = mock(InterviewTurnRepository.class);

        AiConfiguration config = new AiConfiguration("test-key", "gemini-3.8-flash");
        gradingService = new AiGradingService(
                config,
                transport,
                rubricRepository,
                sessionRubricScoreRepository,
                sessionRepository,
                turnRepository
        );

        subject = Subject.builder().id(1).code("SWD392").name("Software Architecture").build();
        session = InterviewSession.builder()
                .id(10)
                .subject(subject)
                .student(User.builder().id(100).username("se123").build())
                .build();

        rubrics = List.of(
                Rubric.builder().id(1).subject(subject).criterion("Hiểu kiến trúc Layered").keywords("controller,service,repository").maxScore(BigDecimal.valueOf(5.0)).sortOrder(1).build(),
                Rubric.builder().id(2).subject(subject).criterion("Phân tích REST API").keywords("http,get,post,json").maxScore(BigDecimal.valueOf(5.0)).sortOrder(2).build()
        );

        turns = List.of(
                InterviewTurn.builder().id(101).session(session).mainIndex(1).followupIndex(0).turnType(TurnType.MAIN)
                        .questionText("Kiến trúc Layered gồm những tầng nào?")
                        .transcript("Gồm controller, service và repository giao tiếp với cơ sở dữ liệu.").build(),
                InterviewTurn.builder().id(102).session(session).mainIndex(2).followupIndex(0).turnType(TurnType.MAIN)
                        .questionText("REST API sử dụng các phương thức nào?")
                        .transcript("REST sử dụng HTTP POST, GET để truyền nhận dữ liệu định dạng JSON.").build()
        );

        when(rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(1)).thenReturn(rubrics);
        when(turnRepository.findBySessionIdOrderByMainIndexAscFollowupIndexAsc(10)).thenReturn(turns);
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRubricScoreRepository.save(any(SessionRubricScore.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testEvaluateWithGeminiSuccess() throws Exception {
        String mockGeminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"overall_feedback\\": \\"Sinh viên nắm chắc kiến thức cơ bản về Layered và REST API.\\", \\"evaluations\\": [{\\"rubric_id\\": 1, \\"ai_score\\": 4.5, \\"comment\\": \\"Nêu đủ các tầng\\"}, {\\"rubric_id\\": 2, \\"ai_score\\": 4.0, \\"comment\\": \\"Nêu đúng HTTP methods\\"}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        when(transport.post(anyString(), anyString())).thenReturn(mockGeminiResponse);

        InterviewSession evaluated = gradingService.evaluateSession(session);

        assertNotNull(evaluated.getAiSuggestedScore());
        assertEquals(BigDecimal.valueOf(8.5).setScale(2), evaluated.getAiSuggestedScore());
        assertTrue(evaluated.getAiFeedback().contains("Sinh viên nắm chắc"));
        verify(sessionRubricScoreRepository, times(2)).save(any(SessionRubricScore.class));
    }

    @Test
    void testEvaluateFallbackWhenGeminiThrowsException() throws Exception {
        when(transport.post(anyString(), anyString())).thenThrow(new GenerationException("Quota exceeded"));

        InterviewSession evaluated = gradingService.evaluateSession(session);

        assertNotNull(evaluated.getAiSuggestedScore());
        assertTrue(evaluated.getAiSuggestedScore().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(evaluated.getAiFeedback());
        verify(sessionRubricScoreRepository, times(2)).save(any(SessionRubricScore.class));
    }

    @Test
    void testEvaluateSessionWithoutRubrics() {
        when(rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(1)).thenReturn(Collections.emptyList());

        InterviewSession evaluated = gradingService.evaluateSession(session);

        assertEquals(BigDecimal.ZERO, evaluated.getAiSuggestedScore());
        assertTrue(evaluated.getAiFeedback().contains("chưa được thiết lập"));
        assertEquals(AiGradingService.SOURCE_NO_RUBRIC, evaluated.getAiGradingSource());
    }

    @Test
    void testGeminiPercentScoresStrengthsAndEvidence() throws Exception {
        String inner = "{\\\"overall_feedback\\\": \\\"Hiểu bài tốt.\\\", \\\"strengths\\\": [\\\"Nêu đủ các tầng\\\"], \\\"weaknesses\\\": [\\\"Chưa nói về PUT\\\"],"
                + " \\\"evaluations\\\": [{\\\"rubric_id\\\": 1, \\\"score_percent\\\": 90, \\\"comment\\\": \\\"Đủ tầng\\\", \\\"evidence\\\": \\\"controller, service và repository\\\"},"
                + " {\\\"rubric_id\\\": 2, \\\"score_percent\\\": 50, \\\"comment\\\": \\\"Thiếu PUT\\\", \\\"evidence\\\": \\\"\\\"}]}";
        when(transport.post(anyString(), anyString()))
                .thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + inner + "\"}]}}]}");

        InterviewSession evaluated = gradingService.evaluateSession(session);

        // 90% của 5đ = 4.5; 50% của 5đ = 2.5; tổng 7/10
        assertEquals(new BigDecimal("7.00"), evaluated.getAiSuggestedScore());
        assertEquals(AiGradingService.SOURCE_GEMINI, evaluated.getAiGradingSource());
        assertTrue(evaluated.getAiFeedback().contains("Điểm mạnh: Nêu đủ các tầng"));
        assertTrue(evaluated.getAiFeedback().contains("Điểm yếu: Chưa nói về PUT"));
        ArgumentCaptor<SessionRubricScore> saved = ArgumentCaptor.forClass(SessionRubricScore.class);
        verify(sessionRubricScoreRepository, times(2)).save(saved.capture());
        assertEquals(new BigDecimal("4.50"), saved.getAllValues().get(0).getAiScore());
        assertTrue(saved.getAllValues().get(0).getComment().contains("Bằng chứng: “controller, service và repository”"));
    }

    @Test
    void testAiTotalIsNormalizedToTenPointScale() throws Exception {
        rubrics = List.of(
                Rubric.builder().id(1).subject(subject).criterion("A").keywords("controller").maxScore(BigDecimal.valueOf(2.0)).sortOrder(1).build(),
                Rubric.builder().id(2).subject(subject).criterion("B").keywords("json").maxScore(BigDecimal.valueOf(2.0)).sortOrder(2).build()
        );
        when(rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(1)).thenReturn(rubrics);
        when(transport.post(anyString(), anyString())).thenThrow(new GenerationException("timeout"));

        InterviewSession evaluated = gradingService.evaluateSession(session);

        // Rubric tổng 4đ, đạt đủ 4đ -> quy về thang 10 = 10.00
        assertEquals(new BigDecimal("10.00"), evaluated.getAiSuggestedScore());
        assertEquals(AiGradingService.SOURCE_RULE_BASED, evaluated.getAiGradingSource());
        assertTrue(evaluated.getAiFeedback().startsWith("[Chấm dự phòng theo từ khoá"));
    }

    @Test
    void testLockedSessionIsNotOverwrittenByAi() throws Exception {
        session.setFinalScore(new BigDecimal("6.00"));
        session.setAiSuggestedScore(new BigDecimal("9.00"));

        InterviewSession evaluated = gradingService.evaluateSession(session);

        assertEquals(new BigDecimal("9.00"), evaluated.getAiSuggestedScore());
        verify(transport, never()).post(anyString(), anyString());
        verify(sessionRubricScoreRepository, never()).deleteBySessionId(anyInt());
    }
}
