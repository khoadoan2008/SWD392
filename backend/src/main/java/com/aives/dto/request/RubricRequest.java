package com.aives.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RubricRequest {

    @NotBlank(message = "Tiêu chí không được để trống")
    private String criterion;

    private String keywords;

    @NotNull(message = "Điểm tối đa không được để trống")
    @DecimalMin(value = "0.1", message = "Điểm tối đa phải lớn hơn 0")
    private BigDecimal maxScore;

    private Integer sortOrder = 0;
}
