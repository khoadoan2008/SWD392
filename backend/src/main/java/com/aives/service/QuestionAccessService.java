package com.aives.service;

import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import com.aives.repository.SubjectRepository;
import com.aives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionAccessService {
    private final UserRepository users;
    private final SubjectRepository subjects;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public User actor(int id) {
        User actor = users.findById(id)
            .orElseThrow(() -> new AccessDeniedException("Tài khoản không hoạt động."));
        // Open-in-view can retain a user across the provider call; reload security state.
        entityManager.refresh(actor);
        if (!actor.isActive()) throw new AccessDeniedException("Tài khoản không hoạt động.");
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.LECTURER)
            throw new AccessDeniedException("Chỉ giảng viên hoặc quản trị viên.");
        return actor;
    }

    @Transactional(readOnly = true)
    public Subject requireSubject(int actorId, int subjectId) {
        User actor = actor(actorId);
        Subject subject = subjects.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Môn học không tồn tại hoặc ngừng hoạt động."));
        entityManager.refresh(subject);
        if (!subject.isActive()) throw new IllegalArgumentException("Môn học không tồn tại hoặc ngừng hoạt động.");
        if (actor.getRole() != Role.ADMIN && subjects.findByLecturerId(actorId).stream().noneMatch(s -> s.getId().equals(subjectId)))
            throw new AccessDeniedException("Bạn chưa được phân công môn học này.");
        return subject;
    }

    @Transactional(readOnly = true)
    public List<Subject> manageable(int actorId) {
        User actor = actor(actorId);
        return actor.getRole() == Role.ADMIN ? subjects.findByActiveTrueOrderByCodeAsc()
            : subjects.findByLecturerId(actorId).stream().filter(Subject::isActive)
                .sorted(java.util.Comparator.comparing(Subject::getCode)).toList();
    }
}
