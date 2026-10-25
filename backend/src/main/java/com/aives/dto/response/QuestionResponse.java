package com.aives.dto.response;

import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.DifficultyLevel;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.QuestionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record QuestionResponse(
        Integer id,
        SubjectSummary subject,
        String topic,
        String content,
        String referenceAnswer,
        BloomLevel bloomLevel,
        DifficultyLevel difficulty,
        String notes,
        QuestionStatus status,
        QuestionSource source,
        UserSummary createdBy,
        UserSummary reviewedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<RubricSummary> rubrics
) {
    public record SubjectSummary(Integer id, String code, String name, String description) {
        public static SubjectSummary of(Subject s) {
            return s == null ? null : new SubjectSummary(s.getId(), s.getCode(), s.getName(), s.getDescription());
        }
    }

    public record UserSummary(Integer id, String fullName, String username) {
        public static UserSummary of(User u) {
            return u == null ? null : new UserSummary(u.getId(), u.getFullName(), u.getUsername());
        }
    }

    public record RubricSummary(Integer id, String criterion, String keywords, BigDecimal maxScore, Integer sortOrder) {
        public static RubricSummary of(Rubric r) {
            return r == null ? null : new RubricSummary(r.getId(), r.getCriterion(), r.getKeywords(), r.getMaxScore(), r.getSortOrder());
        }
    }

    public static QuestionResponse of(Question q) {
        if (q == null) return null;
        List<RubricSummary> rubricsList = Collections.emptyList();
        if (q.getSubject() != null && q.getSubject().getRubrics() != null) {
            rubricsList = q.getSubject().getRubrics().stream().map(RubricSummary::of).toList();
        }
        return new QuestionResponse(
                q.getId(),
                SubjectSummary.of(q.getSubject()),
                q.getTopic(),
                q.getContent(),
                q.getReferenceAnswer(),
                q.getBloomLevel(),
                q.getDifficulty(),
                q.getNotes(),
                q.getStatus(),
                q.getSource(),
                UserSummary.of(q.getCreatedBy()),
                UserSummary.of(q.getReviewedBy()),
                q.getCreatedAt(),
                q.getUpdatedAt(),
                rubricsList
        );
    }
}
