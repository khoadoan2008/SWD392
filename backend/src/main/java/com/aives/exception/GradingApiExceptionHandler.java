package com.aives.exception;

import com.aives.controller.LecturerSessionController;
import com.aives.dto.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Chỉ áp dụng cho API buổi thi của giảng viên; chỉ bắt GradingException nên không ảnh hưởng xử lý lỗi khác. */
@RestControllerAdvice(assignableTypes = LecturerSessionController.class)
@Order(-1)
public class GradingApiExceptionHandler {

    @ExceptionHandler(GradingException.class)
    public ResponseEntity<ApiResponse<Void>> grading(GradingException error) {
        return ResponseEntity.status(error.getStatus()).body(ApiResponse.error(error.getMessage()));
    }
}
