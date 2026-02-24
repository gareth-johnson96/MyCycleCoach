package com.mycyclecoach.feature.strava.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.mycyclecoach.config.StravaConfig;
import com.mycyclecoach.feature.strava.client.StravaApiClient;
import com.mycyclecoach.feature.strava.domain.StravaConnection;
import com.mycyclecoach.feature.strava.dto.StravaConnectionResponse;
import com.mycyclecoach.feature.strava.dto.StravaTokenResponse;
import com.mycyclecoach.feature.strava.exception.StravaConnectionNotFoundException;
import com.mycyclecoach.feature.strava.repository.StravaConnectionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StravaAuthServiceImplTest {

    @Mock
    private StravaConfig stravaConfig;

    @Mock
    private StravaApiClient stravaApiClient;

    @Mock
    private StravaConnectionRepository stravaConnectionRepository;

    @InjectMocks
    private StravaAuthServiceImpl stravaAuthService;

    @Test
    void shouldGenerateAuthorizationUrlWithCorrectParameters() {
        // given
        given(stravaConfig.getAuthorizationUrl()).willReturn("https://www.strava.com/oauth/authorize");
        given(stravaConfig.getClientId()).willReturn("test-client-id");
        given(stravaConfig.getRedirectUri()).willReturn("http://localhost:8080/callback");

        // when
        String authUrl = stravaAuthService.generateAuthorizationUrl();

        // then
        assertThat(authUrl).contains("https://www.strava.com/oauth/authorize");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=http://localhost:8080/callback");
        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("scope=read,activity:read_all");
    }

    @Test
    void shouldCreateNewConnectionWhenHandlingOAuthCallback() {
        // given
        Long userId = 1L;
        String code = "auth-code";
        Long athleteId = 12345L;

        StravaTokenResponse tokenResponse = new StravaTokenResponse(
                "access-token",
                "refresh-token",
                System.currentTimeMillis() / 1000 + 3600,
                new StravaTokenResponse.StravaAthlete(athleteId));

        given(stravaApiClient.exchangeCodeForToken(code)).willReturn(tokenResponse);
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(stravaConnectionRepository.save(any(StravaConnection.class))).willAnswer(invocation -> {
            StravaConnection conn = invocation.getArgument(0);
            conn.setId(1L);
            return conn;
        });

        // when
        StravaConnectionResponse response = stravaAuthService.handleOAuthCallback(userId, code);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.stravaAthleteId()).isEqualTo(athleteId);
        assertThat(response.connected()).isTrue();
        then(stravaConnectionRepository).should().save(any(StravaConnection.class));
    }

    @Test
    void shouldThrowExceptionWhenConnectionNotFoundForGetStatus() {
        // given
        Long userId = 1L;
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> stravaAuthService.getConnectionStatus(userId))
                .isInstanceOf(StravaConnectionNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void shouldReturnConnectionStatusWhenConnectionExists() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .id(1L)
                .userId(userId)
                .stravaAthleteId(12345L)
                .accessToken("token")
                .refreshToken("refresh")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));

        // when
        StravaConnectionResponse response = stravaAuthService.getConnectionStatus(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.stravaAthleteId()).isEqualTo(12345L);
        assertThat(response.connected()).isTrue();
    }

    @Test
    void shouldDeleteConnectionWhenDisconnecting() {
        // given
        Long userId = 1L;
        StravaConnection connection =
                StravaConnection.builder().id(1L).userId(userId).build();

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));

        // when
        stravaAuthService.disconnect(userId);

        // then
        then(stravaConnectionRepository).should().delete(connection);
    }

    @Test
    void shouldRefreshTokenWhenExpired() {
        // given
        Long userId = 1L;
        String oldAccessToken = "old-access-token";
        String oldRefreshToken = "old-refresh-token";
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);

        StravaConnection connection = StravaConnection.builder()
                .id(1L)
                .userId(userId)
                .accessToken(oldAccessToken)
                .refreshToken(oldRefreshToken)
                .expiresAt(expiredTime)
                .build();

        StravaTokenResponse tokenResponse = new StravaTokenResponse(
                "new-access-token",
                "new-refresh-token",
                System.currentTimeMillis() / 1000 + 3600,
                new StravaTokenResponse.StravaAthlete(12345L));

        given(stravaConfig.getTokenRefreshBufferSeconds()).willReturn(3600);
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        given(stravaApiClient.refreshAccessToken(oldRefreshToken)).willReturn(tokenResponse);
        given(stravaConnectionRepository.save(any(StravaConnection.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaAuthService.refreshTokenIfNeeded(userId);

        // then
        then(stravaApiClient).should().refreshAccessToken(oldRefreshToken);
        then(stravaConnectionRepository).should().save(any(StravaConnection.class));
    }

    @Test
    void shouldRefreshTokenWhenExpiringWithinBuffer() {
        // given
        Long userId = 1L;
        String oldAccessToken = "old-access-token";
        String oldRefreshToken = "old-refresh-token";
        // Token expires in 30 minutes, but buffer is 1 hour
        LocalDateTime expiringTime = LocalDateTime.now().plusMinutes(30);

        StravaConnection connection = StravaConnection.builder()
                .id(1L)
                .userId(userId)
                .accessToken(oldAccessToken)
                .refreshToken(oldRefreshToken)
                .expiresAt(expiringTime)
                .build();

        StravaTokenResponse tokenResponse = new StravaTokenResponse(
                "new-access-token",
                "new-refresh-token",
                System.currentTimeMillis() / 1000 + 3600,
                new StravaTokenResponse.StravaAthlete(12345L));

        given(stravaConfig.getTokenRefreshBufferSeconds()).willReturn(3600); // 1 hour buffer
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        given(stravaApiClient.refreshAccessToken(oldRefreshToken)).willReturn(tokenResponse);
        given(stravaConnectionRepository.save(any(StravaConnection.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaAuthService.refreshTokenIfNeeded(userId);

        // then
        then(stravaApiClient).should().refreshAccessToken(oldRefreshToken);
        then(stravaConnectionRepository).should().save(any(StravaConnection.class));
    }

    @Test
    void shouldNotRefreshTokenWhenNotExpired() {
        // given
        Long userId = 1L;
        // Token expires in 2 hours, buffer is 1 hour - no refresh needed
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);

        StravaConnection connection = StravaConnection.builder()
                .id(1L)
                .userId(userId)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresAt(futureTime)
                .build();

        given(stravaConfig.getTokenRefreshBufferSeconds()).willReturn(3600);
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));

        // when
        stravaAuthService.refreshTokenIfNeeded(userId);

        // then
        then(stravaApiClient).should(never()).refreshAccessToken(anyString());
        then(stravaConnectionRepository).should(never()).save(any(StravaConnection.class));
    }
}
