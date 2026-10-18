package com.aives.controller;

import com.aives.dto.request.CreateUserRequest;
import com.aives.dto.request.UpdateUserRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.UserResponse;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import com.aives.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String q) {
        List<User> list = userService.searchUsers(role, q);
        List<UserResponse> responses = list.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countStats() {
        return ResponseEntity.ok(ApiResponse.success(userService.countStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(userService.getUserById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        User u = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .studentCode(request.getStudentCode())
                .active(request.isActive())
                .build();

        User saved = userService.createUser(u, request.getPassword(), request.getSubjectIds());
        return ResponseEntity.ok(ApiResponse.success("Đã tạo người dùng thành công", toResponse(saved)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        User u = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .studentCode(request.getStudentCode())
                .active(request.isActive())
                .build();

        User updated = userService.updateUser(id, u, request.getNewPassword(), request.getSubjectIds());
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật người dùng thành công", toResponse(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá người dùng thành công"));
    }

    private UserResponse toResponse(User u) {
        List<Integer> subIds = (u.getAssignedSubjects() != null)
                ? u.getAssignedSubjects().stream().map(Subject::getId).toList()
                : List.of();

        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .studentCode(u.getStudentCode())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .assignedSubjectIds(subIds)
                .build();
    }
}
