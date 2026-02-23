package com.mycyclecoach.feature.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.dto.AuthResponse;
import com.mycyclecoach.feature.auth.dto.LoginRequest;
import com.mycyclecoach.feature.auth.dto.RefreshTokenRequest;
import com.mycyclecoach.feature.auth.dto.RegisterRequest;
import com.mycyclecoach.feature.auth.exception.InvalidCredentialsException;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import com.mycyclecoach.feature.auth.exception.UserAlreadyExistsException;
import com.mycyclecoach.feature.auth.security.JwtAuthenticationFilter;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        then(authService).should().registerUser(request);
    }

    @Test
    void shouldReturn400WhenRegisteringUserWithExistingEmail() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        doThrow(new UserAlreadyExistsException("User with email existing@example.com already exists"))
                .when(authService)
                .registerUser(request);

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRegisteringWithInvalidEmail() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("invalid-email", "password123");

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRegisteringWithBlankPassword() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@example.com", "");

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse response = new AuthResponse("access.token", "refresh.token", 3600L, "Bearer");
        given(authService.authenticateUser(request)).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        then(authService).should().authenticateUser(request);
    }

    @Test
    void shouldReturn400WhenLoginWithInvalidCredentials() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        given(authService.authenticateUser(request))
                .willThrow(new InvalidCredentialsException("Invalid email or password"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLoginWithInvalidEmail() throws Exception {
        // given
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLoginWithBlankPassword() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "");

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("old.refresh.token");
        AuthResponse response = new AuthResponse("new.access.token", "new.refresh.token", 3600L, "Bearer");
        given(authService.refreshToken(request.refreshToken())).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        then(authService).should().refreshToken(request.refreshToken());
    }

    @Test
    void shouldReturn400WhenRefreshingWithInvalidToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.token");
        given(authService.refreshToken(request.refreshToken()))
                .willThrow(new InvalidCredentialsException("Invalid refresh token"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRefreshingWithExpiredToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("expired.token");
        given(authService.refreshToken(request.refreshToken()))
                .willThrow(new TokenExpiredException("Refresh token has expired"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRefreshingWithBlankToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // when / then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
