package com.aives.dto.response;

import com.aives.model.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

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
}
