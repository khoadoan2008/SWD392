package com.aives.service;

import com.aives.model.entity.Subject;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Subject> getActiveSubjects() {
        return subjectRepository.findByActiveTrueOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByLecturer(Integer lecturerId) {
        return subjectRepository.findByLecturerId(lecturerId);
    }

    @Transactional(readOnly = true)
    public Subject getSubjectById(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + id));
    }

    @Transactional
    public Subject saveSubject(Subject subject) {
        String code = subject.getCode().trim().toUpperCase();
        subject.setCode(code);

        if (subject.getId() == null) {
            if (subjectRepository.existsByCodeIgnoreCase(code)) {
                throw new IllegalArgumentException("Mã môn học " + code + " đã tồn tại.");
            }
        } else {
            subjectRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
                if (!existing.getId().equals(subject.getId())) {
                    throw new IllegalArgumentException("Mã môn học " + code + " đã được sử dụng bởi môn khác.");
                }
            });
        }

        return subjectRepository.save(subject);
    }

    @Transactional
    public void deleteSubject(Integer id) {
        if (!subjectRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy môn học ID: " + id);
        }
        subjectRepository.deleteById(id);
    }
}
