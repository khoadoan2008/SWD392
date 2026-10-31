package com.aives.controller;

import com.aives.dto.request.GradeSessionRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.InterviewSessionResponse;
import com.aives.security.UserPrincipal;
import com.aives.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lecturer/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
public class LecturerSessionController {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewSessionResponse>>> searchSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer subjectId) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.searchSessions(principal.getUser(), subjectId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.countSessionStats(principal.getUser())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> getSessionDetail(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getSessionDetail(id)));
    }

    @PostMapping("/{id}/grade")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> gradeSession(
            @PathVariable Integer id,
            @Valid @RequestBody GradeSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Lưu điểm chính thức thành công",
                interviewService.gradeSession(id, request, principal.getUser())));
    }

    @PostMapping("/{id}/re-evaluate")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> reEvaluateSession(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("AI đã đánh giá lại buổi thi theo rubric",
                interviewService.reEvaluateSession(id, principal.getUser())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        interviewService.deleteSession(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá lượt thi thành công"));
    }
}
