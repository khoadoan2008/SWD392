package com.aives.controller;

import com.aives.dto.request.CreateUserRequest;
import com.aives.dto.request.UpdateUserRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.StudentImportResult;
import com.aives.dto.response.UserResponse;
import com.aives.model.enums.Role;
import com.aives.service.StudentImportService;
import com.aives.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final StudentImportService studentImportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String q) {
        List<UserResponse> responses = userService.searchUsers(role, q);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countStats() {
        return ResponseEntity.ok(ApiResponse.success(userService.countStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo người dùng thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật người dùng thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá người dùng thành công"));
    }

    @Operation(summary = "Tải file Excel mẫu để import danh sách sinh viên")
    @GetMapping(value = "/import-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
        byte[] bytes = studentImportService.generateExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_import_sinh_vien.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "Import danh sách sinh viên hàng loạt từ file Excel (.xlsx, .xls) hoặc CSV (.csv)")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StudentImportResult>> importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "defaultPassword", required = false) String defaultPassword,
            @RequestParam(value = "updateExisting", required = false, defaultValue = "false") boolean updateExisting) {
        StudentImportResult result = studentImportService.importStudents(file, subjectId, defaultPassword, updateExisting);
        return ResponseEntity.ok(ApiResponse.success("Xử lý import danh sách sinh viên hoàn tất", result));
    }
}
