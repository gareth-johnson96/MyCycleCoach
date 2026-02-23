package com.mycyclecoach.feature.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Configure JwtConfig with test values using lenient stubbing
        lenient()
                .when(jwtConfig.getSecret())
                .thenReturn("test-secret-key-that-is-long-enough-for-hs512-algorithm-requirements-minimum-512-bits");
        lenient().when(jwtConfig.getAccessTokenTtl()).thenReturn(3600000L); // 1 hour
        lenient().when(jwtConfig.getRefreshTokenTtl()).thenReturn(86400000L); // 24 hours

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        // given
        Long userId = 123L;

        // when
        String token = jwtTokenProvider.generateAccessToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        // given
        Long userId = 123L;

        // when
        String token = jwtTokenProvider.generateRefreshToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldExtractUserIdFromValidToken() {
        // given
        Long userId = 123L;
        String token = jwtTokenProvider.generateAccessToken(userId);

        // when
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldValidateValidToken() {
        // given
        Long userId = 123L;
        String token = jwtTokenProvider.generateAccessToken(userId);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalseForMalformedToken() {
        // given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // when
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalseForEmptyToken() {
        // given
        String emptyToken = "";

        // when
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldThrowTokenExpiredExceptionForExpiredToken() {
        // given - create a provider with very short TTL
        when(jwtConfig.getSecret())
                .thenReturn("test-secret-key-that-is-long-enough-for-hs512-algorithm-requirements-minimum-512-bits");
        when(jwtConfig.getAccessTokenTtl()).thenReturn(1L); // 1 millisecond
        lenient().when(jwtConfig.getRefreshTokenTtl()).thenReturn(86400000L);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(jwtConfig);
        Long userId = 123L;
        String token = shortLivedProvider.generateAccessToken(userId);

        // wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when / then
        assertThatThrownBy(() -> shortLivedProvider.validateToken(token))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Token has expired");
    }

    @Test
    void shouldDetectExpiredToken() {
        // given - create a provider with very short TTL
        when(jwtConfig.getSecret())
                .thenReturn("test-secret-key-that-is-long-enough-for-hs512-algorithm-requirements-minimum-512-bits");
        when(jwtConfig.getAccessTokenTtl()).thenReturn(1L); // 1 millisecond
        lenient().when(jwtConfig.getRefreshTokenTtl()).thenReturn(86400000L);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(jwtConfig);
        Long userId = 123L;
        String token = shortLivedProvider.generateAccessToken(userId);

        // wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when
        boolean isExpired = shortLivedProvider.isTokenExpired(token);

        // then
        assertThat(isExpired).isTrue();
    }

    @Test
    void shouldNotDetectNonExpiredToken() {
        // given
        Long userId = 123L;
        String token = jwtTokenProvider.generateAccessToken(userId);

        // when
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // then
        assertThat(isExpired).isFalse();
    }

    @Test
    void shouldHandleBase64EncodedSecret() {
        // given - Base64 encoded 512-bit secret
        String base64Secret =
                "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzNTEyLWFsZ29yaXRobS1yZXF1aXJlbWVudHMtbWluaW11bS01MTItYml0cw==";
        when(jwtConfig.getSecret()).thenReturn(base64Secret);
        lenient().when(jwtConfig.getAccessTokenTtl()).thenReturn(3600000L);
        lenient().when(jwtConfig.getRefreshTokenTtl()).thenReturn(86400000L);
        JwtTokenProvider providerWithBase64Secret = new JwtTokenProvider(jwtConfig);
        Long userId = 456L;

        // when
        String token = providerWithBase64Secret.generateAccessToken(userId);
        boolean isValid = providerWithBase64Secret.validateToken(token);
        Long extractedUserId = providerWithBase64Secret.getUserIdFromToken(token);

        // then
        assertThat(token).isNotNull();
        assertThat(isValid).isTrue();
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldPadShortSecret() {
        // given - secret shorter than 512 bits
        String shortSecret = "short-secret";
        when(jwtConfig.getSecret()).thenReturn(shortSecret);
        lenient().when(jwtConfig.getAccessTokenTtl()).thenReturn(3600000L);
        lenient().when(jwtConfig.getRefreshTokenTtl()).thenReturn(86400000L);
        JwtTokenProvider providerWithShortSecret = new JwtTokenProvider(jwtConfig);
        Long userId = 789L;

        // when
        String token = providerWithShortSecret.generateAccessToken(userId);
        boolean isValid = providerWithShortSecret.validateToken(token);
        Long extractedUserId = providerWithShortSecret.getUserIdFromToken(token);

        // then
        assertThat(token).isNotNull();
        assertThat(isValid).isTrue();
        assertThat(extractedUserId).isEqualTo(userId);
    }
}
