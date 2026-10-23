package com.aives.controller;

import com.aives.dto.request.GenerateQuestionRequest;
import com.aives.dto.request.QuestionRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.GenerationSubjectResponse;
import com.aives.dto.response.QuestionResponse;
import com.aives.dto.response.QuestionView;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionStatus;
import com.aives.security.UserPrincipal;
import com.aives.service.QuestionService;
import com.aives.service.AiQuestionGenerationService;
import com.aives.service.QuestionAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lecturer/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
public class LecturerQuestionController {

    private final QuestionService questionService;
    private final AiQuestionGenerationService aiGeneration;
    private final QuestionAccessService generationAccess;

    @GetMapping("/generation-subjects")
    public ResponseEntity<ApiResponse<List<GenerationSubjectResponse>>> generationSubjects(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<GenerationSubjectResponse> subjects = generationAccess.manageable(principal.getUser().getId()).stream()
                .map(GenerationSubjectResponse::of)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> searchQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) BloomLevel bloom,
            @RequestParam(required = false) String q) {
        List<QuestionResponse> questions = questionService.searchQuestions(
                principal.getUser(), subjectId, status, bloom, q);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(questionService.countStats(principal.getUser())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(questionService.getQuestionById(id, principal.getUser())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse saved = questionService.createQuestion(request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Đã tạo câu hỏi thành công", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse updated = questionService.updateQuestion(id, request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật câu hỏi thành công", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @RequestParam QuestionStatus to) {
        QuestionResponse updated = questionService.updateStatus(id, to, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển trạng thái câu hỏi: " + to.getLabel(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        questionService.deleteQuestion(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá câu hỏi thành công"));
    }

    @PostMapping(value = "/generate", consumes = "application/json")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> generateAiQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateQuestionRequest request) {
        List<QuestionResponse> generated = questionService.generateAiQuestions(
                request.getSubjectId(), request.getTopic(), request.getLevels(), principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Đã tạo " + generated.size() + " câu theo mẫu (không gọi AI)", generated));
    }

    @PostMapping(value = "/generate", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<List<QuestionView>>> generateFromMaterial(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int subjectId, @RequestParam String topic, @RequestParam List<BloomLevel> levels,
            @RequestParam boolean allowAiTransfer, @RequestPart("materialFile") MultipartFile file,
            @RequestHeader("Idempotency-Key") String requestId) throws Exception {
        var generated = aiGeneration.generate(principal.getUser().getId(), subjectId, topic, levels,
                allowAiTransfer, file, requestId);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu câu hỏi Gemini, đáp án và rubric 10 điểm ở trạng thái chờ duyệt.", generated));
    }
}
