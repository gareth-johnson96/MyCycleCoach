package com.mycyclecoach.feature.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, Long expiresIn, String tokenType) {}
