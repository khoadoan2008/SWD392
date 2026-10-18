package com.aives.dto.request;

import com.aives.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String email;

    @NotNull(message = "Vai trò không được để trống")
    private Role role;

    private String studentCode;

    private boolean active = true;

    private String newPassword;

    private List<Integer> subjectIds;
}
