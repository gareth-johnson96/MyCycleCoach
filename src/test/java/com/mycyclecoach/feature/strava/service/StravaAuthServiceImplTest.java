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
                "access-token", "refresh-token", System.currentTimeMillis() / 1000 + 3600,
                new StravaTokenResponse.StravaAthlete(athleteId));

        given(stravaApiClient.exchangeCodeForToken(code)).willReturn(tokenResponse);
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(stravaConnectionRepository.save(any(StravaConnection.class)))
                .willAnswer(invocation -> {
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
        StravaConnection connection = StravaConnection.builder()
                .id(1L)
                .userId(userId)
                .build();

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));

        // when
        stravaAuthService.disconnect(userId);

        // then
        then(stravaConnectionRepository).should().delete(connection);
    }
}
