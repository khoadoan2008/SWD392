package com.aives.dto.response;
import com.aives.model.entity.*;
import com.aives.model.enums.*;
import com.aives.model.entity.Question;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record QuestionView(Integer id,Integer subjectId,String subjectName,String topic,String content,
        String referenceAnswer,BloomLevel bloomLevel,QuestionStatus status,QuestionSource source,
        LocalDateTime createdAt,List<CriterionView> rubrics) {
    public record CriterionView(Integer id,String criterion,String keywords,BigDecimal maxScore,int sortOrder) { }
    public static QuestionView of(Question question) {
        return new QuestionView(question.getId(),question.getSubject().getId(),question.getSubject().getName(),question.getTopic(),
            question.getContent(),question.getReferenceAnswer(),question.getBloomLevel(),question.getStatus(),question.getSource(),question.getCreatedAt(),
            question.getRubrics().stream().map(r->new CriterionView(r.getId(),r.getCriterion(),r.getKeywords(),r.getMaxScore(),r.getSortOrder())).toList());
    }
}
