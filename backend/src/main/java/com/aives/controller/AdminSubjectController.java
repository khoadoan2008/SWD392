package com.aives.controller;

import com.aives.dto.request.SubjectRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.model.entity.Subject;
import com.aives.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAllSubjects() {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getAllSubjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getSubjectById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getSubjectById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Subject>> createSubject(@Valid @RequestBody SubjectRequest request) {
        Subject s = Subject.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .active(request.isActive())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Đã tạo môn học thành công", subjectService.saveSubject(s)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> updateSubject(
            @PathVariable Integer id,
            @Valid @RequestBody SubjectRequest request) {
        Subject s = subjectService.getSubjectById(id);
        s.setCode(request.getCode());
        s.setName(request.getName());
        s.setDescription(request.getDescription());
        s.setActive(request.isActive());
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật môn học thành công", subjectService.saveSubject(s)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Integer id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá môn học thành công"));
    }
}
