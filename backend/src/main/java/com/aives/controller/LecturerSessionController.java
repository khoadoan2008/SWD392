package com.aives.controller;

import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.InterviewSessionResponse;
import com.aives.security.UserPrincipal;
import com.aives.service.InterviewService;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        interviewService.deleteSession(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá lượt thi thành công"));
    }
}
