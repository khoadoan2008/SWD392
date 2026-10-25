package com.aives.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeSessionRequest {

    @NotNull(message = "Điểm chốt không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm phải >= 0")
    @DecimalMax(value = "10.0", message = "Điểm phải <= 10")
    private BigDecimal finalScore;

    private String lecturerFeedback;

    @Valid
    private List<RubricGradeItem> rubricGrades;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RubricGradeItem {
        @NotNull(message = "Mã rubric không được để trống")
        private Integer rubricId;

        @DecimalMin(value = "0.0", message = "Điểm rubric phải >= 0")
        private BigDecimal finalScore;

        private String comment;
    }
}
