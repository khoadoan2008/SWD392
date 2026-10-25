package com.aives.dto.response;

import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String studentCode;
    private boolean active;
    private LocalDateTime createdAt;
    private List<Integer> assignedSubjectIds;

    public static UserResponse of(User u) {
        if (u == null) return null;
        List<Integer> subIds = (u.getAssignedSubjects() != null)
                ? u.getAssignedSubjects().stream().map(Subject::getId).toList()
                : List.of();

        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .studentCode(u.getStudentCode())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .assignedSubjectIds(subIds)
                .build();
    }
}
