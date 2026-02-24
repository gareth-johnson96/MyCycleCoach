package com.mycyclecoach.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.controller.AuthController;
import com.mycyclecoach.feature.auth.dto.LoginRequest;
import com.mycyclecoach.feature.auth.dto.RegisterRequest;
import com.mycyclecoach.feature.auth.exception.EmailNotVerifiedException;
import com.mycyclecoach.feature.auth.exception.InvalidCredentialsException;
import com.mycyclecoach.feature.auth.exception.InvalidVerificationTokenException;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import com.mycyclecoach.feature.auth.exception.UserAlreadyExistsException;
import com.mycyclecoach.feature.auth.security.JwtAuthenticationFilter;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

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
    void shouldHandleValidationException() throws Exception {
        // given - invalid request with missing email
        String invalidJson = "{\"email\": \"\", \"password\": \"test123\"}";

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleUserAlreadyExistsException() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        doThrow(new UserAlreadyExistsException("User with email existing@example.com already exists"))
                .when(authService)
                .registerUser(request);

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("User with email existing@example.com already exists"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleInvalidCredentialsException() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        given(authService.authenticateUser(request))
                .willThrow(new InvalidCredentialsException("Invalid email or password"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleTokenExpiredException() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        given(authService.authenticateUser(request)).willThrow(new TokenExpiredException("Token has expired"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Token has expired"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        given(authService.authenticateUser(request)).willThrow(new RuntimeException("Unexpected error"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldIncludeAllFieldsInErrorResponse() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("invalid-email", "");

        // when
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ErrorResponse errorResponse = objectMapper.readValue(response, ErrorResponse.class);

        // then
        assertThat(errorResponse.status()).isEqualTo(400);
        assertThat(errorResponse.error()).isEqualTo("Bad Request");
        assertThat(errorResponse.message()).isNotBlank();
        assertThat(errorResponse.path()).isEqualTo("/api/v1/auth/register");
        assertThat(errorResponse.timestamp()).isNotNull();
    }

    @Test
    void shouldHandleEmailNotVerifiedException() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        given(authService.authenticateUser(request))
                .willThrow(new EmailNotVerifiedException("Please verify your email address"));

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Please verify your email address"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleInvalidVerificationTokenException() throws Exception {
        // given
        doThrow(new InvalidVerificationTokenException("Invalid verification token"))
                .when(authService)
                .verifyEmail("invalid-token");

        // when / then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid verification token"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/verify"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
