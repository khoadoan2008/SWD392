package com.aives.dto.response;

import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.QuestionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record QuestionView(
        Integer id,
        Integer subjectId,
        String subjectName,
        String topic,
        String content,
        String referenceAnswer,
        BloomLevel bloomLevel,
        QuestionStatus status,
        QuestionSource source,
        LocalDateTime createdAt,
        List<CriterionView> rubrics
) {
    public record CriterionView(Integer id, String criterion, String keywords, BigDecimal maxScore, int sortOrder) { }

    public static QuestionView of(Question question) {
        List<Rubric> rubricList = (question.getRubrics() != null && !question.getRubrics().isEmpty())
                ? question.getRubrics()
                : (question.getSubject() != null && question.getSubject().getRubrics() != null
                    ? question.getSubject().getRubrics()
                    : Collections.emptyList());

        return new QuestionView(
                question.getId(),
                question.getSubject().getId(),
                question.getSubject().getName(),
                question.getTopic(),
                question.getContent(),
                question.getReferenceAnswer(),
                question.getBloomLevel(),
                question.getStatus(),
                question.getSource(),
                question.getCreatedAt(),
                rubricList.stream().map(r -> new CriterionView(
                        r.getId(),
                        r.getCriterion(),
                        r.getKeywords(),
                        r.getMaxScore(),
                        r.getSortOrder() != null ? r.getSortOrder() : 0
                )).toList()
        );
    }
}
