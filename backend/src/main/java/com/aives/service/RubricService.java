package com.aives.service;

import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.aives.repository.QuestionRepository;
import com.aives.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RubricService {

    private final RubricRepository rubricRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<Rubric> getRubricsByQuestion(Integer questionId) {
        return rubricRepository.findByQuestionIdOrderBySortOrderAscIdAsc(questionId);
    }

    @Transactional
    public Rubric saveRubric(Integer questionId, Rubric rubric) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + questionId));

        if (rubric.getCriterion() == null || rubric.getCriterion().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu chí không được để trống.");
        }
        if (rubric.getMaxScore() == null || rubric.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm tối đa phải là số dương lớn hơn 0.");
        }

        rubric.setQuestion(question);
        return rubricRepository.save(rubric);
    }

    @Transactional
    public void deleteRubric(Integer id) {
        rubricRepository.deleteById(id);
    }
}
