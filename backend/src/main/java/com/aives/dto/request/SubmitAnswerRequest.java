package com.aives.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitAnswerRequest {

    @NotNull(message = "Turn ID không được để trống")
    private Integer turnId;

    private String transcript;
}
