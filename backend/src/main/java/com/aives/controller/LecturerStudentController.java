package com.aives.controller;

import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.StudentImportResult;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import com.aives.security.UserPrincipal;
import com.aives.service.StudentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/lecturer/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
@Tag(name = "Lecturer Students", description = "Dành cho Giảng viên: Import danh sách sinh viên lớp thi qua Excel/CSV")
public class LecturerStudentController {

    private final StudentImportService studentImportService;

    @Operation(summary = "Tải file Excel mẫu để import danh sách sinh viên")
    @GetMapping(value = "/import-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] bytes = studentImportService.generateExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_import_sinh_vien.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "Giảng viên import danh sách sinh viên vào môn thi phụ trách từ file Excel/CSV")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StudentImportResult>> importStudents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "defaultPassword", required = false) String defaultPassword,
            @RequestParam(value = "updateExisting", required = false, defaultValue = "false") boolean updateExisting) {

        User currentUser = principal.getUser();
        if (currentUser.getRole() == Role.LECTURER && subjectId != null) {
            boolean isAssigned = currentUser.getAssignedSubjects() != null &&
                    currentUser.getAssignedSubjects().stream().anyMatch(s -> s.getId().equals(subjectId));
            if (!isAssigned) {
                throw new AccessDeniedException("Bạn không được phân công phụ trách môn học ID: " + subjectId);
            }
        }

        StudentImportResult result = studentImportService.importStudents(file, subjectId, defaultPassword, updateExisting);
        return ResponseEntity.ok(ApiResponse.success("Giảng viên import danh sách sinh viên thành công", result));
    }
}
