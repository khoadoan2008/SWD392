package com.aives.service;

import com.aives.dto.request.LoginRequest;
import com.aives.dto.response.AuthResponse;
import com.aives.dto.response.UserResponse;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);

    UserResponse getCurrentUserProfile(String username);
}
