package com.aives.service.impl;

import com.aives.dto.request.LoginRequest;
import com.aives.dto.response.AuthResponse;
import com.aives.dto.response.UserResponse;
import com.aives.model.entity.User;
import com.aives.repository.UserRepository;
import com.aives.security.JwtTokenProvider;
import com.aives.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        // 1. Xác thực username và password qua AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername().trim(),
                        loginRequest.getPassword()
                )
        );

        // 2. Tìm entity User tương ứng trong database
        User user = userRepository.findByUsername(loginRequest.getUsername().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng!"));

        // 3. Kiểm tra trạng thái kích hoạt của tài khoản
        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản của bạn hiện đang bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        // 4. Sinh JWT token
        String jwt = tokenProvider.generateToken(authentication);

        // 5. Chuyển đổi Entity sang DTO an toàn (DTO Pattern)
        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng!"));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        List<Integer> subIds = (user.getAssignedSubjects() != null)
                ? user.getAssignedSubjects().stream().map(com.aives.model.entity.Subject::getId).toList()
                : java.util.List.of();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .studentCode(user.getStudentCode())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .assignedSubjectIds(subIds)
                .build();
    }
}
