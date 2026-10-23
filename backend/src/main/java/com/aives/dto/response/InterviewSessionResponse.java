package com.aives.dto.response;

import com.aives.model.entity.InterviewSession;
import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.SessionStatus;
import com.aives.model.enums.TurnType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record InterviewSessionResponse(
        Integer id,
        StudentSummary student,
        SubjectSummary subject,
        SessionStatus status,
        Integer answerTimeLimitSec,
        Integer maxFollowups,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        List<TurnSummary> turns
) {
    public record StudentSummary(Integer id, String username, String fullName, String email, String studentCode) {
        public static StudentSummary of(User u) {
            return u == null ? null : new StudentSummary(
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getStudentCode()
            );
        }
    }

    public record SubjectSummary(Integer id, String code, String name, String description) {
        public static SubjectSummary of(Subject s) {
            return s == null ? null : new SubjectSummary(
                    s.getId(),
                    s.getCode(),
                    s.getName(),
                    s.getDescription()
            );
        }
    }

    public record TurnSummary(
            Integer id,
            Integer mainIndex,
            Integer followupIndex,
            TurnType turnType,
            String questionText,
            String transcript,
            Integer responseTimeSec,
            LocalDateTime askedAt,
            LocalDateTime answeredAt,
            boolean isFollowUp
    ) {
        public static TurnSummary of(InterviewTurn t) {
            if (t == null) return null;
            return new TurnSummary(
                    t.getId(),
                    t.getMainIndex(),
                    t.getFollowupIndex(),
                    t.getTurnType(),
                    t.getQuestionText(),
                    t.getTranscript(),
                    t.getResponseTimeSec(),
                    t.getAskedAt(),
                    t.getAnsweredAt(),
                    t.isFollowUp()
            );
        }
    }

    public static InterviewSessionResponse of(InterviewSession s) {
        if (s == null) return null;
        return new InterviewSessionResponse(
                s.getId(),
                StudentSummary.of(s.getStudent()),
                SubjectSummary.of(s.getSubject()),
                s.getStatus(),
                s.getAnswerTimeLimitSec(),
                s.getMaxFollowups(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getTurns() != null
                        ? s.getTurns().stream().map(TurnSummary::of).toList()
                        : Collections.emptyList()
        );
    }
}
