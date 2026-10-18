package com.aives.model.entity;

import com.aives.model.enums.TurnType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonBackReference
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_turn_id")
    private InterviewTurn parentTurn;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_type", nullable = false, length = 10)
    private TurnType turnType;

    @Column(name = "main_index", nullable = false)
    private Integer mainIndex;

    @Column(name = "followup_index", nullable = false)
    @Builder.Default
    private Integer followupIndex = 0;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "response_time_sec")
    private Integer responseTimeSec;

    @Column(name = "asked_at")
    private LocalDateTime askedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    public boolean isFollowUp() {
        return turnType == TurnType.FOLLOW_UP || (followupIndex != null && followupIndex > 0);
    }
}
