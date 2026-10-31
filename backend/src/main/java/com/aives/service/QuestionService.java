package com.aives.service;

import com.aives.dto.request.QuestionRequest;
import com.aives.dto.response.QuestionResponse;
import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionStatus;
import com.aives.model.enums.QuestionSource;
import com.aives.model.entity.Role;
import com.aives.repository.QuestionRepository;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionApprovalPolicy approvalPolicy;
    private final SubjectRepository subjectRepository;
    private final QuestionGenerator questionGenerator;

    @Transactional(readOnly = true)
    public List<QuestionResponse> searchQuestions(User currentUser, Integer subjectId, QuestionStatus status, BloomLevel bloom, String kw) {
        List<Integer> allowedSubjectIds = getAllowedSubjectIds(currentUser);
        return questionRepository.search(allowedSubjectIds, subjectId, status, bloom, kw).stream()
                .map(QuestionResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countStats(User currentUser) {
        List<Integer> allowedSubjectIds = getAllowedSubjectIds(currentUser);
        Map<String, Long> stats = new HashMap<>();
        for (QuestionStatus s : QuestionStatus.values()) {
            stats.put(s.name(), 0L);
        }
        List<Object[]> rows = questionRepository.countByStatus(allowedSubjectIds);
        long total = 0;
        for (Object[] r : rows) {
            QuestionStatus st = (QuestionStatus) r[0];
            Long count = (Long) r[1];
            stats.put(st.name(), count);
            total += count;
        }
        stats.put("TOTAL", total);
        return stats;
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Integer id, User currentUser) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(currentUser, q.getSubject().getId());
        return QuestionResponse.of(q);
    }

    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request, User currentUser) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (request.getBloomLevel() == null) {
            throw new IllegalArgumentException("Vui lòng chọn mức độ nhận thức Bloom.");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + request.getSubjectId()));
        checkSubjectPermission(currentUser, subject.getId());

        if (request.getStatus() == QuestionStatus.APPROVED)
            approvalPolicy.requireApprovable(subject.getId(), QuestionSource.MANUAL);

        Question q = Question.builder()
                .subject(subject)
                .topic(request.getTopic())
                .content(request.getContent().trim())
                .referenceAnswer(request.getReferenceAnswer())
                .bloomLevel(request.getBloomLevel())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : com.aives.model.enums.DifficultyLevel.MEDIUM)
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : QuestionStatus.DRAFT)
                .source(QuestionSource.MANUAL)
                .createdBy(currentUser)
                .reviewedBy(request.getStatus() == QuestionStatus.APPROVED ? currentUser : null)
                .build();

        return QuestionResponse.of(questionRepository.save(q));
    }

    @Transactional
    public QuestionResponse updateQuestion(Integer id, QuestionRequest request, User currentUser) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (request.getBloomLevel() == null) {
            throw new IllegalArgumentException("Vui lòng chọn mức độ nhận thức Bloom.");
        }

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(currentUser, q.getSubject().getId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + request.getSubjectId()));
        checkSubjectPermission(currentUser, subject.getId());

        q.setSubject(subject);
        q.setTopic(request.getTopic());
        q.setContent(request.getContent().trim());
        q.setReferenceAnswer(request.getReferenceAnswer());
        q.setBloomLevel(request.getBloomLevel());
        if (request.getDifficulty() != null) q.setDifficulty(request.getDifficulty());
        // Omitted fields from older clients must not erase imported metadata.
        if (request.getNotes() != null) q.setNotes(request.getNotes());
        QuestionStatus target = request.getStatus() != null ? request.getStatus() : q.getStatus();
        if (target == QuestionStatus.APPROVED) {
            approvalPolicy.requireApprovable(subject.getId(), q.getSource());
            q.setReviewedBy(currentUser);
        } else if (target == QuestionStatus.DRAFT) q.setReviewedBy(null);
        if (request.getStatus() != null) {
            q.setStatus(request.getStatus());
        }

        return QuestionResponse.of(questionRepository.save(q));
    }

    @Transactional
    public QuestionResponse updateStatus(Integer id, QuestionStatus newStatus, User reviewer) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(reviewer, q.getSubject().getId());

        if (newStatus == QuestionStatus.APPROVED) {
            approvalPolicy.requireApprovable(q.getSubject().getId(), q.getSource());
            q.setReviewedBy(reviewer);
        } else if (newStatus == QuestionStatus.DRAFT) {
            q.setReviewedBy(null);
        } else {
            q.setReviewedBy(reviewer);
        }

        q.setStatus(newStatus);
        return QuestionResponse.of(questionRepository.save(q));
    }

    @Transactional
    public void deleteQuestion(Integer id, User currentUser) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(currentUser, q.getSubject().getId());
        questionRepository.delete(q);
    }

    @Transactional
    public List<QuestionResponse> generateAiQuestions(Integer subjectId, String topic, List<BloomLevel> levels, User currentUser) {
        checkSubjectPermission(currentUser, subjectId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + subjectId));

        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Chủ đề không được để trống.");
        }
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("Cần chọn ít nhất 1 mức nhận thức Bloom.");
        }

        List<Question> generated = questionGenerator.generate(subject, topic.trim(), levels);
        for (Question q : generated) {
            q.setCreatedBy(currentUser);
        }
        return questionRepository.saveAll(generated).stream().map(QuestionResponse::of).toList();
    }

    public List<Integer> getAllowedSubjectIds(User user) {
        if (user == null || user.isAdmin()) {
            return null; // Admin has access to all
        }
        List<Integer> ids = new ArrayList<>();
        // The JWT principal is detached: query current assignments, not its lazy collection.
        for (Subject s : subjectRepository.findByLecturerId(user.getId())) ids.add(s.getId());
        return ids.isEmpty() ? List.of(-1) : ids; // If no subjects assigned, return dummy id
    }

    public void checkSubjectPermission(User user, Integer subjectId) {
        if (user == null || user.isAdmin()) {
            return;
        }
        List<Integer> allowed = getAllowedSubjectIds(user);
        if (allowed == null || !allowed.contains(subjectId)) {
            throw new SecurityException("Bạn không được phân công giảng dạy môn học này.");
        }
    }
}
