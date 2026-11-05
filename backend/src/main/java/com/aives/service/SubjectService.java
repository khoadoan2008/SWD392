package com.aives.service;

import com.aives.dto.request.SubjectRequest;
import com.aives.dto.response.SubjectResponse;
import com.aives.model.entity.Subject;
import com.aives.repository.InterviewSessionRepository;
import com.aives.repository.QuestionRepository;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream().map(SubjectResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getActiveSubjects() {
        return subjectRepository.findByActiveTrueOrderByCodeAsc().stream().map(SubjectResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByLecturer(Integer lecturerId) {
        return subjectRepository.findByLecturerId(lecturerId).stream().map(SubjectResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Integer id) {
        return SubjectResponse.of(findSubjectEntity(id));
    }

    @Transactional(readOnly = true)
    public Subject findSubjectEntity(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + id));
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (subjectRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã môn học " + code + " đã tồn tại.");
        }

        Subject s = Subject.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.isActive())
                .build();

        return SubjectResponse.of(subjectRepository.save(s));
    }

    @Transactional
    public SubjectResponse updateSubject(Integer id, SubjectRequest request) {
        Subject s = findSubjectEntity(id);
        String code = request.getCode().trim().toUpperCase();

        subjectRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Mã môn học " + code + " đã được sử dụng bởi môn khác.");
            }
        });

        s.setCode(code);
        s.setName(request.getName().trim());
        s.setDescription(request.getDescription());
        s.setActive(request.isActive());

        return SubjectResponse.of(subjectRepository.save(s));
    }

    @Transactional
    public void deleteSubject(Integer id) {
        if (!subjectRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy môn học ID: " + id);
        }
        if (questionRepository.existsBySubjectId(id) || sessionRepository.existsBySubjectId(id)) {
            throw new IllegalStateException("Môn học đã có câu hỏi hoặc lượt thi, không thể xoá. Hãy chuyển môn sang trạng thái ngừng hoạt động.");
        }
        subjectRepository.deleteById(id);
    }
}
