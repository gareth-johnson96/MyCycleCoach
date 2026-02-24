package com.mycyclecoach.feature.auth.service;

import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.domain.RefreshToken;
import com.mycyclecoach.feature.auth.domain.User;
import com.mycyclecoach.feature.auth.dto.AuthResponse;
import com.mycyclecoach.feature.auth.dto.LoginRequest;
import com.mycyclecoach.feature.auth.dto.RegisterRequest;
import com.mycyclecoach.feature.auth.exception.EmailNotVerifiedException;
import com.mycyclecoach.feature.auth.exception.InvalidCredentialsException;
import com.mycyclecoach.feature.auth.exception.InvalidVerificationTokenException;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import com.mycyclecoach.feature.auth.exception.UserAlreadyExistsException;
import com.mycyclecoach.feature.auth.repository.RefreshTokenRepository;
import com.mycyclecoach.feature.auth.repository.UserRepository;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.userprofile.domain.UserProfile;
import com.mycyclecoach.feature.userprofile.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

        // Create user entity
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(tokenExpiry)
                .build();
        User savedUser = userRepository.save(user);

        // Create empty user profile
        UserProfile profile = UserProfile.builder().userId(savedUser.getId()).build();
        userProfileRepository.save(profile);

        // Send verification email
        emailService.sendVerificationEmail(request.email(), verificationToken);

        log.info("User registered successfully: {}", request.email());
    }

    @Override
    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Please verify your email address before logging in. Check your inbox for the verification link.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = generateAndSaveRefreshToken(user.getId());

        log.info("User authenticated successfully: {}", request.email());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtConfig.getAccessTokenTtl() / 1000, // Convert to seconds
                "Bearer");
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException("Refresh token has expired");
        }

        Long userId = token.getUserId();
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = generateAndSaveRefreshToken(userId);

        // Delete old refresh token
        refreshTokenRepository.delete(token);

        log.info("Token refreshed successfully for user: {}", userId);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                jwtConfig.getAccessTokenTtl() / 1000, // Convert to seconds
                "Bearer");
    }

    @Override
    public void validateToken(String token) {
        jwtTokenProvider.validateToken(token);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository
                .findByVerificationToken(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationTokenException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    private String generateAndSaveRefreshToken(Long userId) {
        String token = jwtTokenProvider.generateRefreshToken(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenTtl() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
