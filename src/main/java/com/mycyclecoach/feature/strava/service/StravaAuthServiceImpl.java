package com.mycyclecoach.feature.strava.service;

import com.mycyclecoach.config.StravaConfig;
import com.mycyclecoach.feature.strava.client.StravaApiClient;
import com.mycyclecoach.feature.strava.domain.StravaConnection;
import com.mycyclecoach.feature.strava.dto.StravaConnectionResponse;
import com.mycyclecoach.feature.strava.dto.StravaTokenResponse;
import com.mycyclecoach.feature.strava.exception.StravaConnectionNotFoundException;
import com.mycyclecoach.feature.strava.repository.StravaConnectionRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class StravaAuthServiceImpl implements StravaAuthService {

    private final StravaConfig stravaConfig;
    private final StravaApiClient stravaApiClient;
    private final StravaConnectionRepository stravaConnectionRepository;

    @Override
    public String generateAuthorizationUrl() {
        log.info("Generating Strava authorization URL");

        return UriComponentsBuilder.fromUriString(stravaConfig.getAuthorizationUrl())
                .queryParam("client_id", stravaConfig.getClientId())
                .queryParam("redirect_uri", stravaConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "read,activity:read_all")
                .build()
                .toUriString();
    }

    @Override
    @Transactional
    public StravaConnectionResponse handleOAuthCallback(Long userId, String code) {
        log.info("Handling OAuth callback for user: {}", userId);

        StravaTokenResponse tokenResponse = stravaApiClient.exchangeCodeForToken(code);

        LocalDateTime expiresAt =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(tokenResponse.expiresAt()), ZoneId.systemDefault());

        StravaConnection connection = stravaConnectionRepository
                .findByUserId(userId)
                .map(existing -> {
                    existing.setAccessToken(tokenResponse.accessToken());
                    existing.setRefreshToken(tokenResponse.refreshToken());
                    existing.setExpiresAt(expiresAt);
                    existing.setStravaAthleteId(tokenResponse.athlete().id());
                    existing.setScope("read,activity:read_all");
                    return existing;
                })
                .orElseGet(() -> StravaConnection.builder()
                        .userId(userId)
                        .stravaAthleteId(tokenResponse.athlete().id())
                        .accessToken(tokenResponse.accessToken())
                        .refreshToken(tokenResponse.refreshToken())
                        .expiresAt(expiresAt)
                        .scope("read,activity:read_all")
                        .build());

        StravaConnection savedConnection = stravaConnectionRepository.save(connection);

        log.info("Successfully connected Strava account for user: {}", userId);

        return new StravaConnectionResponse(
                savedConnection.getId(),
                savedConnection.getUserId(),
                savedConnection.getStravaAthleteId(),
                true,
                savedConnection.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public StravaConnectionResponse getConnectionStatus(Long userId) {
        log.info("Getting Strava connection status for user: {}", userId);

        StravaConnection connection = stravaConnectionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new StravaConnectionNotFoundException(userId));

        return new StravaConnectionResponse(
                connection.getId(),
                connection.getUserId(),
                connection.getStravaAthleteId(),
                true,
                connection.getCreatedAt());
    }

    @Override
    @Transactional
    public void disconnect(Long userId) {
        log.info("Disconnecting Strava for user: {}", userId);

        StravaConnection connection = stravaConnectionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new StravaConnectionNotFoundException(userId));

        stravaConnectionRepository.delete(connection);

        log.info("Successfully disconnected Strava for user: {}", userId);
    }

    @Override
    @Transactional
    public void refreshTokenIfNeeded(Long userId) {
        log.info("Checking if token refresh is needed for user: {}", userId);

        StravaConnection connection = stravaConnectionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new StravaConnectionNotFoundException(userId));

        if (connection.isExpired(stravaConfig.getTokenRefreshBufferSeconds())) {
            log.info("Token expired or expiring soon, refreshing for user: {}", userId);

            StravaTokenResponse tokenResponse = stravaApiClient.refreshAccessToken(connection.getRefreshToken());

            LocalDateTime expiresAt =
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(tokenResponse.expiresAt()), ZoneId.systemDefault());

            connection.setAccessToken(tokenResponse.accessToken());
            connection.setRefreshToken(tokenResponse.refreshToken());
            connection.setExpiresAt(expiresAt);

            stravaConnectionRepository.save(connection);

            log.info("Token refreshed successfully for user: {}", userId);
        } else {
            log.debug("Token still valid for user: {}, no refresh needed", userId);
        }
    }
}
