package com.aives.dto.request;

import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.DifficultyLevel;
import jakarta.validation.constraints.Size;
import com.aives.model.enums.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

    @NotNull(message = "Môn học không được để trống")
    private Integer subjectId;

    private String topic;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    private String referenceAnswer;

    @NotNull(message = "Mức độ nhận thức Bloom không được để trống")
    private BloomLevel bloomLevel;

    private DifficultyLevel difficulty;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String notes;

    private QuestionStatus status;
}
