package com.aives.dto.request;

import com.aives.model.enums.BloomLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GenerateQuestionRequest {

    @NotNull(message = "Môn học không được để trống")
    private Integer subjectId;

    @NotBlank(message = "Chủ đề không được để trống")
    private String topic;

    @NotEmpty(message = "Cần chọn ít nhất một mức độ Bloom")
    private List<BloomLevel> levels;
}
