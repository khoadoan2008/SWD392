package com.aives.controller;

import com.aives.dto.request.RubricRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.RubricResponse;
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

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<List<RubricResponse>>> getRubricsBySubject(@PathVariable Integer subjectId) {
        return ResponseEntity.ok(ApiResponse.success(rubricService.getRubricsBySubject(subjectId)));
    }

    @PostMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<RubricResponse>> createRubric(
            @PathVariable Integer subjectId,
            @Valid @RequestBody RubricRequest request) {
        RubricResponse response = rubricService.createRubric(subjectId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm tiêu chí Rubric cho môn học", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RubricResponse>> updateRubric(
            @PathVariable Integer id,
            @RequestParam Integer subjectId,
            @Valid @RequestBody RubricRequest request) {
        RubricResponse response = rubricService.updateRubric(id, subjectId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật tiêu chí Rubric cho môn học", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRubric(@PathVariable Integer id) {
        rubricService.deleteRubric(id);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá tiêu chí Rubric"));
    }
}
