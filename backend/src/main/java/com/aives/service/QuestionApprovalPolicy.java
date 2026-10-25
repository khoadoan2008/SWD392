package com.aives.service;

import com.aives.model.entity.Rubric;
import com.aives.model.enums.QuestionSource;
import com.aives.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/** Shared by manual editing, the review route and Excel import. */
@Service
@RequiredArgsConstructor
public class QuestionApprovalPolicy {
    private final RubricRepository rubrics;

    public void requireApprovable(Integer subjectId, QuestionSource source) {
        List<Rubric> criteria = rubrics.findBySubjectIdOrderBySortOrderAscIdAsc(subjectId);
        if (criteria.isEmpty())
            throw new IllegalArgumentException("Cần có ít nhất 1 tiêu chí Rubric cho môn học trước khi duyệt câu hỏi.");
        if (source == QuestionSource.AI && criteria.stream().map(Rubric::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.TEN) != 0)
            throw new IllegalArgumentException("Rubric môn học cho câu hỏi AI cần có tổng 10 điểm trước khi duyệt.");
    }
}
