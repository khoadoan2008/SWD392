package com.aives.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequest {

    @NotBlank(message = "Mã môn học không được để trống")
    @Size(max = 20, message = "Mã môn học tối đa 20 ký tự")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    private String name;

    private String description;

    private boolean active = true;
}
