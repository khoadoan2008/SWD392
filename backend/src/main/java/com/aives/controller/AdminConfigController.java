package com.aives.controller;

import com.aives.dto.response.ApiResponse;
import com.aives.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    private final SystemConfigService configService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.success(configService.getAllConfigs()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> updateConfigs(@RequestBody Map<String, String> configs) {
        configService.updateConfigs(configs);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật cấu hình hệ thống thành công", configService.getAllConfigs()));
    }
}
