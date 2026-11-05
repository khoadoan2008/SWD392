package com.aives.service;

import com.aives.dto.request.RubricRequest;
import com.aives.dto.response.RubricResponse;
import com.aives.model.entity.Rubric;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.repository.RubricRepository;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RubricService {

    private final RubricRepository rubricRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<RubricResponse> getRubricsBySubject(Integer subjectId) {
        return rubricRepository.findBySubjectIdOrderBySortOrderAscIdAsc(subjectId).stream()
                .map(RubricResponse::of)
                .toList();
    }

    @Transactional
    public RubricResponse createRubric(User actor, Integer subjectId, RubricRequest request) {
        requireAssigned(actor, subjectId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + subjectId));

        if (request.getCriterion() == null || request.getCriterion().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu chí không được để trống.");
        }
        if (request.getMaxScore() == null || request.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm tối đa phải là số dương lớn hơn 0.");
        }

        Rubric rubric = Rubric.builder()
                .subject(subject)
                .criterion(request.getCriterion().trim())
                .keywords(request.getKeywords())
                .maxScore(request.getMaxScore())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        return RubricResponse.of(rubricRepository.save(rubric));
    }

    @Transactional
    public RubricResponse updateRubric(User actor, Integer id, Integer subjectId, RubricRequest request) {
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tiêu chí Rubric ID: " + id));
        if (rubric.getSubject() != null) requireAssigned(actor, rubric.getSubject().getId());
        requireAssigned(actor, subjectId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + subjectId));

        if (request.getCriterion() == null || request.getCriterion().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu chí không được để trống.");
        }
        if (request.getMaxScore() == null || request.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm tối đa phải là số dương lớn hơn 0.");
        }

        rubric.setSubject(subject);
        rubric.setCriterion(request.getCriterion().trim());
        rubric.setKeywords(request.getKeywords());
        rubric.setMaxScore(request.getMaxScore());
        rubric.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        return RubricResponse.of(rubricRepository.save(rubric));
    }

    @Transactional
    public void deleteRubric(User actor, Integer id) {
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tiêu chí Rubric ID: " + id));
        if (rubric.getSubject() != null) requireAssigned(actor, rubric.getSubject().getId());
        rubricRepository.delete(rubric);
    }

    private void requireAssigned(User actor, Integer subjectId) {
        if (actor == null || actor.isAdmin()) return;
        boolean assigned = subjectRepository.findByLecturerId(actor.getId()).stream()
                .anyMatch(s -> s.getId().equals(subjectId));
        if (!assigned) throw new SecurityException("Bạn không được phân công giảng dạy môn học này.");
    }
}
