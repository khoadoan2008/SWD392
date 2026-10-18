package com.aives.service;

import com.aives.dto.response.UserResponse;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import com.aives.repository.SubjectRepository;
import com.aives.repository.UserRepository;
import com.aives.security.Pbkdf2CompatiblePasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final Pbkdf2CompatiblePasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> searchUsers(Role role, String q) {
        return userRepository.searchUsers(role, q);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL", userRepository.count());
        stats.put("ADMIN", userRepository.countByRole(Role.ADMIN));
        stats.put("LECTURER", userRepository.countByRole(Role.LECTURER));
        stats.put("STUDENT", userRepository.countByRole(Role.STUDENT));
        return stats;
    }

    @Transactional(readOnly = true)
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
    }

    @Transactional
    public User createUser(User user, String rawPassword, List<Integer> subjectIds) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự.");
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        if (user.getRole() == Role.LECTURER && subjectIds != null && !subjectIds.isEmpty()) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(subjectIds));
            user.setAssignedSubjects(subjects);
        } else {
            user.setAssignedSubjects(new HashSet<>());
        }

        if (user.getRole() != Role.STUDENT) {
            user.setStudentCode(null);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Integer id, User userUpdate, String newPassword, List<Integer> subjectIds) {
        User user = getUserById(id);

        user.setFullName(userUpdate.getFullName());
        user.setEmail(userUpdate.getEmail());
        user.setActive(userUpdate.isActive());
        user.setRole(userUpdate.getRole());

        if (user.getRole() == Role.STUDENT) {
            user.setStudentCode(userUpdate.getStudentCode());
        } else {
            user.setStudentCode(null);
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.trim().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự.");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        }

        if (user.getRole() == Role.LECTURER && subjectIds != null) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(subjectIds));
            user.setAssignedSubjects(subjects);
        } else {
            user.getAssignedSubjects().clear();
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}
