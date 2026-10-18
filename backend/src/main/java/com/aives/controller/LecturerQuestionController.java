package com.aives.controller;

import com.aives.dto.request.GenerateQuestionRequest;
import com.aives.dto.request.QuestionRequest;
import com.aives.dto.response.ApiResponse;
import com.aives.dto.response.QuestionView;
import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionStatus;
import com.aives.security.UserPrincipal;
import com.aives.service.QuestionService;
import com.aives.service.SubjectService;
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
    private final SubjectService subjectService;
    private final AiQuestionGenerationService aiGeneration;
    private final QuestionAccessService generationAccess;

    public record GenerationSubject(Integer id, String code, String name) { }

    @GetMapping("/generation-subjects")
    public ResponseEntity<ApiResponse<List<GenerationSubject>>> generationSubjects(@AuthenticationPrincipal UserPrincipal principal) {
        var subjects = generationAccess.manageable(principal.getUser().getId()).stream()
            .map(s -> new GenerationSubject(s.getId(), s.getCode(), s.getName())).toList();
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Question>>> searchQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) BloomLevel bloom,
            @RequestParam(required = false) String q) {
        User user = principal.getUser();
        List<Question> questions = questionService.searchQuestions(user, subjectId, status, bloom, q);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        return ResponseEntity.ok(ApiResponse.success(questionService.countStats(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Question>> getQuestionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        User user = principal.getUser();
        return ResponseEntity.ok(ApiResponse.success(questionService.getQuestionById(id, user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Question>> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuestionRequest request) {
        User user = principal.getUser();
        Subject subject = subjectService.getSubjectById(request.getSubjectId());

        Question q = Question.builder()
                .subject(subject)
                .topic(request.getTopic())
                .content(request.getContent())
                .referenceAnswer(request.getReferenceAnswer())
                .bloomLevel(request.getBloomLevel())
                .status(request.getStatus() != null ? request.getStatus() : QuestionStatus.DRAFT)
                .build();

        Question saved = questionService.saveQuestion(q, user);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo câu hỏi thành công", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Question>> updateQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody QuestionRequest request) {
        User user = principal.getUser();
        Subject subject = subjectService.getSubjectById(request.getSubjectId());

        Question q = questionService.getQuestionById(id, user);
        q.setSubject(subject);
        q.setTopic(request.getTopic());
        q.setContent(request.getContent());
        q.setReferenceAnswer(request.getReferenceAnswer());
        q.setBloomLevel(request.getBloomLevel());
        if (request.getStatus() != null) {
            q.setStatus(request.getStatus());
        }

        Question updated = questionService.saveQuestion(q, user);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật câu hỏi thành công", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Question>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @RequestParam QuestionStatus to) {
        User user = principal.getUser();
        Question updated = questionService.updateStatus(id, to, user);
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển trạng thái câu hỏi: " + to.getLabel(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        User user = principal.getUser();
        questionService.deleteQuestion(id, user);
        return ResponseEntity.ok(ApiResponse.successMsg("Đã xoá câu hỏi thành công"));
    }

    @PostMapping(value = "/generate", consumes = "application/json")
    public ResponseEntity<ApiResponse<List<Question>>> generateAiQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateQuestionRequest request) {
        User user = principal.getUser();
        List<Question> generated = questionService.generateAiQuestions(
                request.getSubjectId(), request.getTopic(), request.getLevels(), user);
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
