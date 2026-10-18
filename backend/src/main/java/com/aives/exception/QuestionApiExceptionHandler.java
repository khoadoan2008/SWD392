package com.aives.exception;

import com.aives.controller.LecturerQuestionController;
import com.aives.dto.response.ApiResponse;
import com.aives.service.generation.GenerationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = LecturerQuestionController.class)
@Order(-1)
public class QuestionApiExceptionHandler {
    @ExceptionHandler(GenerationException.class)
    public ResponseEntity<?> generation(GenerationException error) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(error.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(ApiResponse.error(error.getReason()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> size(Exception error) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error("Tệp tối đa 10 MB."));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class, MissingServletRequestPartException.class})
    public ResponseEntity<?> invalid(Exception error) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu hoặc sai tham số yêu cầu."));
    }
}
