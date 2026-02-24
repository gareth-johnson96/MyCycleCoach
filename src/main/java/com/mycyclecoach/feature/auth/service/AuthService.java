package com.mycyclecoach.feature.auth.service;

import com.mycyclecoach.feature.auth.dto.AuthResponse;
import com.mycyclecoach.feature.auth.dto.LoginRequest;
import com.mycyclecoach.feature.auth.dto.RegisterRequest;

public interface AuthService {

    void registerUser(RegisterRequest request);

    AuthResponse authenticateUser(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void validateToken(String token);

    void verifyEmail(String token);
}
