package com.aives.controller;

import com.aives.dto.request.SubmitAnswerRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.model.entity.InterviewSession;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.security.UserPrincipal;
import com.aives.service.InterviewService;
import com.aives.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/interviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
public class StudentInterviewController {

    private final InterviewService interviewService;
    private final SubjectService subjectService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeData(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        List<Subject> activeSubjects = subjectService.getActiveSubjects();
        List<InterviewSession> history = interviewService.getStudentHistory(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("subjects", activeSubjects);
        data.put("history", history);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<InterviewSession>> startInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Integer subjectId) {
        User user = principal.getUser();
        InterviewSession session = interviewService.startSession(user, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu buổi thi vấn đáp", session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentTurn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        User user = principal.getUser();
        Map<String, Object> turnData = interviewService.getCurrentTurnData(id, user);
        return ResponseEntity.ok(ApiResponse.success(turnData));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody SubmitAnswerRequest request) {
        User user = principal.getUser();
        interviewService.submitAnswer(id, request.getTurnId(), request.getTranscript(), user);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã nộp câu trả lời thành công"));
    }

    @PostMapping("/{id}/abort")
    public ResponseEntity<ApiResponse<Void>> abortInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        User user = principal.getUser();
        interviewService.abortSession(id, user);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã huỷ buổi thi"));
    }
}
