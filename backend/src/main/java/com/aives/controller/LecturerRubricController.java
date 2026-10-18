package com.aives.controller;

import com.aives.dto.request.RubricRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.model.entity.Rubric;
import com.aives.service.RubricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecturer/rubrics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
public class LecturerRubricController {

    private final RubricService rubricService;

    @GetMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<List<Rubric>>> getRubricsByQuestion(@PathVariable Integer questionId) {
        return ResponseEntity.ok(ApiResponse.success(rubricService.getRubricsByQuestion(questionId)));
    }

    @PostMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<Rubric>> createRubric(
            @PathVariable Integer questionId,
            @Valid @RequestBody RubricRequest request) {
        Rubric r = Rubric.builder()
                .criterion(request.getCriterion())
                .keywords(request.getKeywords())
                .maxScore(request.getMaxScore())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Đã thêm tiêu chí Rubric", rubricService.saveRubric(questionId, r)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Rubric>> updateRubric(
            @PathVariable Integer id,
            @RequestParam Integer questionId,
            @Valid @RequestBody RubricRequest request) {
        Rubric r = Rubric.builder()
                .id(id)
                .criterion(request.getCriterion())
                .keywords(request.getKeywords())
                .maxScore(request.getMaxScore())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật tiêu chí Rubric", rubricService.saveRubric(questionId, r)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRubric(@PathVariable Integer id) {
        rubricService.deleteRubric(id);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá tiêu chí Rubric"));
    }
}
