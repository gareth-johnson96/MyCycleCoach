package com.mycyclecoach.feature.strava.service;

import com.mycyclecoach.feature.strava.dto.StravaConnectionResponse;

public interface StravaAuthService {

    String generateAuthorizationUrl();

    StravaConnectionResponse handleOAuthCallback(Long userId, String code);

    StravaConnectionResponse getConnectionStatus(Long userId);

    void disconnect(Long userId);

    void refreshTokenIfNeeded(Long userId);
}
