package com.aives.controller;

import com.aives.dto.request.SubmitAnswerRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.InterviewSessionResponse;
import com.aives.dto.response.InterviewTurnDataResponse;
import com.aives.dto.response.StudentHomeResponse;
import com.aives.security.UserPrincipal;
import com.aives.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/interviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
public class StudentInterviewController {

    private final InterviewService interviewService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<StudentHomeResponse>> getHomeData(
            @AuthenticationPrincipal UserPrincipal principal) {
        StudentHomeResponse data = interviewService.getStudentHomeData(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> startInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Integer subjectId) {
        InterviewSessionResponse session = interviewService.startSession(principal.getUser(), subjectId);
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu buổi thi vấn đáp", session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewTurnDataResponse>> getCurrentTurn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        InterviewTurnDataResponse turnData = interviewService.getCurrentTurnData(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(turnData));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody SubmitAnswerRequest request) {
        interviewService.submitAnswer(id, request.getTurnId(), request.getTranscript(), principal.getUser());
        return ResponseEntity.ok(ApiResponse.successMsg("Đã nộp câu trả lời thành công"));
    }

    @PostMapping("/{id}/abort")
    public ResponseEntity<ApiResponse<Void>> abortInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        interviewService.abortSession(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.successMsg("Đã huỷ buổi thi"));
    }
}
