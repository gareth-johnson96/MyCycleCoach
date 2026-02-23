package com.mycyclecoach.feature.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.domain.RefreshToken;
import com.mycyclecoach.feature.auth.domain.User;
import com.mycyclecoach.feature.auth.dto.AuthResponse;
import com.mycyclecoach.feature.auth.dto.LoginRequest;
import com.mycyclecoach.feature.auth.dto.RegisterRequest;
import com.mycyclecoach.feature.auth.exception.InvalidCredentialsException;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import com.mycyclecoach.feature.auth.exception.UserAlreadyExistsException;
import com.mycyclecoach.feature.auth.repository.RefreshTokenRepository;
import com.mycyclecoach.feature.auth.repository.UserRepository;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.userprofile.domain.UserProfile;
import com.mycyclecoach.feature.userprofile.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRegisterNewUserSuccessfully() {
        // given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        String encodedPassword = "encoded-password";
        User user = User.builder()
                .id(1L)
                .email(request.email())
                .passwordHash(encodedPassword)
                .build();

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(userProfileRepository.save(any(UserProfile.class))).willReturn(new UserProfile());

        // when
        authService.registerUser(request);

        // then
        then(userRepository).should().existsByEmail(request.email());
        then(passwordEncoder).should().encode(request.password());
        then(userRepository).should().save(any(User.class));
        then(userProfileRepository).should().save(any(UserProfile.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringUserWithExistingEmail() {
        // given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when / then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should(never()).save(any(User.class));
        then(userProfileRepository).should(never()).save(any(UserProfile.class));
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        String hashedPassword = "$2a$10$hashedpassword";
        User user = User.builder()
                .id(1L)
                .email(request.email())
                .passwordHash(hashedPassword)
                .build();
        String accessToken = "access.jwt.token";
        String refreshToken = "refresh.jwt.token";
        Long accessTtl = 3600000L;

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .willReturn(true);
        given(jwtTokenProvider.generateAccessToken(user.getId())).willReturn(accessToken);
        given(jwtTokenProvider.generateRefreshToken(user.getId())).willReturn(refreshToken);
        given(jwtConfig.getAccessTokenTtl()).willReturn(accessTtl);
        given(jwtConfig.getRefreshTokenTtl()).willReturn(86400000L);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(new RefreshToken());

        // when
        AuthResponse response = authService.authenticateUser(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(response.expiresIn()).isEqualTo(accessTtl / 1000);
        assertThat(response.tokenType()).isEqualTo("Bearer");

        then(userRepository).should().findByEmail(request.email());
        then(passwordEncoder).should().matches(request.password(), user.getPasswordHash());
        then(jwtTokenProvider).should().generateAccessToken(user.getId());
        then(jwtTokenProvider).should().generateRefreshToken(user.getId());
    }

    @Test
    void shouldThrowExceptionWhenAuthenticatingWithInvalidEmail() {
        // given
        LoginRequest request = new LoginRequest("invalid@example.com", "password123");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.authenticateUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        then(userRepository).should().findByEmail(request.email());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(jwtTokenProvider).should(never()).generateAccessToken(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenAuthenticatingWithInvalidPassword() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        User user = User.builder()
                .id(1L)
                .email(request.email())
                .passwordHash("$2a$10$hashedpassword")
                .build();
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .willReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.authenticateUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        then(userRepository).should().findByEmail(request.email());
        then(passwordEncoder).should().matches(request.password(), user.getPasswordHash());
        then(jwtTokenProvider).should(never()).generateAccessToken(anyLong());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // given
        String oldRefreshToken = "old.refresh.token";
        Long userId = 1L;
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(1L)
                .userId(userId)
                .token(oldRefreshToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        Long accessTtl = 3600000L;

        given(refreshTokenRepository.findByToken(oldRefreshToken)).willReturn(Optional.of(refreshTokenEntity));
        given(jwtTokenProvider.generateAccessToken(userId)).willReturn(newAccessToken);
        given(jwtTokenProvider.generateRefreshToken(userId)).willReturn(newRefreshToken);
        given(jwtConfig.getAccessTokenTtl()).willReturn(accessTtl);
        given(jwtConfig.getRefreshTokenTtl()).willReturn(86400000L);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(new RefreshToken());

        // when
        AuthResponse response = authService.refreshToken(oldRefreshToken);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(newAccessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.expiresIn()).isEqualTo(accessTtl / 1000);
        assertThat(response.tokenType()).isEqualTo("Bearer");

        then(refreshTokenRepository).should().findByToken(oldRefreshToken);
        then(refreshTokenRepository).should().delete(refreshTokenEntity);
        then(jwtTokenProvider).should().generateAccessToken(userId);
        then(jwtTokenProvider).should().generateRefreshToken(userId);
    }

    @Test
    void shouldThrowExceptionWhenRefreshingWithInvalidToken() {
        // given
        String invalidRefreshToken = "invalid.refresh.token";
        given(refreshTokenRepository.findByToken(invalidRefreshToken)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(invalidRefreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");

        then(refreshTokenRepository).should().findByToken(invalidRefreshToken);
        then(jwtTokenProvider).should(never()).generateAccessToken(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenRefreshingWithExpiredToken() {
        // given
        String expiredRefreshToken = "expired.refresh.token";
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .token(expiredRefreshToken)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build();

        given(refreshTokenRepository.findByToken(expiredRefreshToken)).willReturn(Optional.of(refreshTokenEntity));

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(expiredRefreshToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Refresh token has expired");

        then(refreshTokenRepository).should().findByToken(expiredRefreshToken);
        then(refreshTokenRepository).should().delete(refreshTokenEntity);
        then(jwtTokenProvider).should(never()).generateAccessToken(anyLong());
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // given
        String token = "valid.jwt.token";
        given(jwtTokenProvider.validateToken(token)).willReturn(true);

        // when
        authService.validateToken(token);

        // then
        then(jwtTokenProvider).should().validateToken(token);
    }
}
