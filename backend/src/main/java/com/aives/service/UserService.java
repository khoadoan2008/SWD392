package com.aives.service;

import com.aives.dto.request.CreateUserRequest;
import com.aives.dto.request.UpdateUserRequest;
import com.aives.dto.response.UserResponse;
import com.aives.model.entity.Role;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.repository.RoleRepository;
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
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final Pbkdf2CompatiblePasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(Role role, String q) {
        return userRepository.searchUsers(role, q).stream()
                .map(UserResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL", userRepository.count());
        stats.put("ADMIN", userRepository.countByRole_NameIgnoreCase("ADMIN"));
        stats.put("LECTURER", userRepository.countByRole_NameIgnoreCase("LECTURER"));
        stats.put("STUDENT", userRepository.countByRole_NameIgnoreCase("STUDENT"));
        return stats;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        return UserResponse.of(findUserEntity(id));
    }

    @Transactional(readOnly = true)
    public User findUserEntity(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự.");
        }

        Role role = roleRepository.findByNameIgnoreCase(request.getRole().getName())
                .orElseGet(() -> roleRepository.save(request.getRole()));

        User user = User.builder()
                .username(request.getUsername().trim())
                .fullName(request.getFullName().trim())
                .email(request.getEmail())
                .role(role)
                .studentCode(role.isStudent() ? request.getStudentCode() : null)
                .active(request.isActive())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        if ((user.isLecturer() || user.isStudent()) && request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            user.setAssignedSubjects(subjects);
        } else {
            user.setAssignedSubjects(new HashSet<>());
        }

        return UserResponse.of(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Integer id, UpdateUserRequest request) {
        User user = findUserEntity(id);

        Role role = roleRepository.findByNameIgnoreCase(request.getRole().getName())
                .orElseGet(() -> roleRepository.save(request.getRole()));

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail());
        user.setActive(request.isActive());
        user.setRole(role);

        if (user.isStudent()) {
            user.setStudentCode(request.getStudentCode());
        } else {
            user.setStudentCode(null);
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        if ((user.isLecturer() || user.isStudent()) && request.getSubjectIds() != null) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            user.setAssignedSubjects(subjects);
        }

        return UserResponse.of(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Integer id) {
        User user = findUserEntity(id);
        userRepository.delete(user);
    }
}
