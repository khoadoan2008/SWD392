package com.aives.service;

import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionStatus;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.Role;
import com.aives.repository.QuestionRepository;
import com.aives.repository.RubricRepository;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final RubricRepository rubricRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionGenerator questionGenerator;

    @Transactional(readOnly = true)
    public List<Question> searchQuestions(User currentUser, Integer subjectId, QuestionStatus status, BloomLevel bloom, String kw) {
        List<Integer> allowedSubjectIds = getAllowedSubjectIds(currentUser);
        return questionRepository.search(allowedSubjectIds, subjectId, status, bloom, kw).stream()
                .map(this::prepareResponse).toList();
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
    public Question getQuestionById(Integer id, User currentUser) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(currentUser, q.getSubject().getId());
        return prepareResponse(q);
    }

    @Transactional
    public Question saveQuestion(Question question, User currentUser) {
        checkSubjectPermission(currentUser, question.getSubject().getId());

        if (question.getContent() == null || question.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (question.getBloomLevel() == null) {
            throw new IllegalArgumentException("Vui lòng chọn mức độ nhận thức Bloom.");
        }

        if (question.getId() == null) {
            question.setCreatedBy(currentUser);
            if (question.getStatus() == null) {
                question.setStatus(QuestionStatus.DRAFT);
            }
        } else {
            Question existing = questionRepository.findById(question.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + question.getId()));
            question.setCreatedBy(existing.getCreatedBy());
            question.setReviewedBy(existing.getReviewedBy());
            question.setCreatedAt(existing.getCreatedAt());
        }

        return prepareResponse(questionRepository.save(question));
    }

    @Transactional
    public Question updateStatus(Integer id, QuestionStatus newStatus, User reviewer) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(reviewer, q.getSubject().getId());

        if (newStatus == QuestionStatus.APPROVED) {
            if (rubricRepository.findByQuestionIdOrderBySortOrderAscIdAsc(id).isEmpty()) {
                throw new IllegalArgumentException("Cần có ít nhất 1 tiêu chí Rubric trước khi duyệt câu hỏi.");
            }
            if (q.getSource() == QuestionSource.AI) {
                BigDecimal total = rubricRepository.findByQuestionIdOrderBySortOrderAscIdAsc(id).stream()
                    .map(r -> r.getMaxScore()).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(BigDecimal.TEN) != 0)
                    throw new IllegalArgumentException("Rubric câu hỏi AI cần có tổng 10 điểm trước khi duyệt.");
            }
            q.setReviewedBy(reviewer);
        } else if (newStatus == QuestionStatus.DRAFT) {
            q.setReviewedBy(null);
        } else {
            q.setReviewedBy(reviewer);
        }

        q.setStatus(newStatus);
        return prepareResponse(questionRepository.save(q));
    }

    @Transactional
    public void deleteQuestion(Integer id, User currentUser) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + id));
        checkSubjectPermission(currentUser, q.getSubject().getId());
        questionRepository.delete(q);
    }

    @Transactional
    public List<Question> generateAiQuestions(Integer subjectId, String topic, List<BloomLevel> levels, User currentUser) {
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
        return questionRepository.saveAll(generated).stream().map(this::prepareResponse).toList();
    }

    // The controller serializes after this transaction ends (open-in-view=false).
    // Load only the relationships in the existing question response contract.
    private Question prepareResponse(Question question) {
        Hibernate.initialize(question.getCreatedBy());
        Hibernate.initialize(question.getReviewedBy());
        Hibernate.initialize(question.getRubrics());
        return question;
    }

    public List<Integer> getAllowedSubjectIds(User user) {
        if (user == null || user.getRole() == Role.ADMIN) {
            return null; // Admin has access to all
        }
        List<Integer> ids = new ArrayList<>();
        // The JWT principal is detached: query current assignments, not its lazy collection.
        for (Subject s : subjectRepository.findByLecturerId(user.getId())) ids.add(s.getId());
        return ids.isEmpty() ? List.of(-1) : ids; // If no subjects assigned, return dummy id
    }

    public void checkSubjectPermission(User user, Integer subjectId) {
        if (user == null || user.getRole() == Role.ADMIN) {
            return;
        }
        List<Integer> allowed = getAllowedSubjectIds(user);
        if (allowed == null || !allowed.contains(subjectId)) {
            throw new SecurityException("Bạn không được phân công giảng dạy môn học này.");
        }
    }
}
