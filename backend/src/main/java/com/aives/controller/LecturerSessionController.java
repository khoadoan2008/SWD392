package com.aives.controller;

import com.aives.dto.request.GradeSessionRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.InterviewSessionResponse;
import com.aives.dto.response.SubjectResponse;
import com.aives.security.UserPrincipal;
import com.aives.service.InterviewService;
import com.aives.service.SubjectService;
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
    private final SubjectService subjectService;

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

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSessionSubjects(
            @AuthenticationPrincipal UserPrincipal principal) {
        var user = principal.getUser();
        return ResponseEntity.ok(ApiResponse.success(user.isAdmin()
                ? subjectService.getAllSubjects()
                : subjectService.getSubjectsByLecturer(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> getSessionDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getSessionDetail(id, principal.getUser())));
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
