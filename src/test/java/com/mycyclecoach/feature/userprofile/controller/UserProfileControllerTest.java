package com.mycyclecoach.feature.userprofile.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.security.JwtAuthenticationFilter;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.userprofile.dto.*;
import com.mycyclecoach.feature.userprofile.service.UserProfileService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldGetProfileSuccessfully() throws Exception {
        // given
        setAuthentication(1L);
        ProfileResponse response =
                new ProfileResponse(1L, 1L, 30, new BigDecimal("75.5"), new BigDecimal("180.0"), "Intermediate", 250, 185);
        given(userProfileService.getProfile(1L)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/v1/user/profile").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.weight").value(75.5))
                .andExpect(jsonPath("$.height").value(180.0))
                .andExpect(jsonPath("$.experienceLevel").value("Intermediate"))
                .andExpect(jsonPath("$.currentFtp").value(250))
                .andExpect(jsonPath("$.maxHr").value(185));

        then(userProfileService).should().getProfile(1L);
    }

    @Test
    void shouldReturn404WhenProfileNotFound() throws Exception {
        // given
        setAuthentication(99L);
        given(userProfileService.getProfile(99L))
                .willThrow(new IllegalArgumentException("User profile not found for userId: 99"));

        // when / then
        mockMvc.perform(get("/api/v1/user/profile").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateProfileSuccessfully() throws Exception {
        // given
        setAuthentication(1L);
        UpdateProfileRequest request = new UpdateProfileRequest(
                32, new BigDecimal("78.0"), new BigDecimal("182.5"), "Advanced", 270, 190);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(userProfileService).should().updateProfile(1L, request);
    }

    @Test
    void shouldReturn400WhenUpdateProfileWithInvalidAge() throws Exception {
        // given
        setAuthentication(1L);
        UpdateProfileRequest request =
                new UpdateProfileRequest(0, new BigDecimal("78.0"), new BigDecimal("182.5"), "Advanced", 270, 190);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdateProfileWithAgeTooHigh() throws Exception {
        // given
        setAuthentication(1L);
        UpdateProfileRequest request =
                new UpdateProfileRequest(200, new BigDecimal("78.0"), new BigDecimal("182.5"), "Advanced", 270, 190);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdateProfileWithInvalidWeight() throws Exception {
        // given
        setAuthentication(1L);
        UpdateProfileRequest request =
                new UpdateProfileRequest(32, new BigDecimal("0"), new BigDecimal("182.5"), "Advanced", 270, 190);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdateProfileWithWeightTooHigh() throws Exception {
        // given
        setAuthentication(1L);
        UpdateProfileRequest request =
                new UpdateProfileRequest(32, new BigDecimal("600"), new BigDecimal("182.5"), "Advanced", 270, 190);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUpdateProfileForNonExistentUser() throws Exception {
        // given
        setAuthentication(99L);
        UpdateProfileRequest request =
                new UpdateProfileRequest(32, new BigDecimal("78.0"), new BigDecimal("182.5"), "Advanced", 270, 190);
        doThrow(new IllegalArgumentException("User profile not found for userId: 99"))
                .when(userProfileService)
                .updateProfile(99L, request);

        // when / then
        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSaveBackgroundSuccessfully() throws Exception {
        // given
        setAuthentication(1L);
        BackgroundRequest request = new BackgroundRequest(
                5,
                10,
                "Trained for 3 months consistently",
                "Sprained ankle last year",
                "None",
                "Tour de France 2020",
                "Monday, Wednesday, Friday",
                "6-8 AM");

        // when / then
        mockMvc.perform(post("/api/v1/user/background")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(userProfileService).should().saveBackground(1L, request);
    }

    @Test
    void shouldUpdateGoalsSuccessfully() throws Exception {
        // given
        setAuthentication(1L);
        GoalsRequest request =
                new GoalsRequest("Complete a century ride", "Gran Fondo", LocalDateTime.of(2025, 6, 15, 0, 0));

        // when / then
        mockMvc.perform(put("/api/v1/user/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(userProfileService).should().updateGoals(1L, request);
    }

    @Test
    void shouldReturn400WhenUpdateGoalsWithBlankGoals() throws Exception {
        // given
        setAuthentication(1L);
        GoalsRequest request = new GoalsRequest("", null, null);

        // when / then
        mockMvc.perform(put("/api/v1/user/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdateGoalsWithNullGoals() throws Exception {
        // given
        setAuthentication(1L);
        GoalsRequest request = new GoalsRequest(null, null, null);

        // when / then
        mockMvc.perform(put("/api/v1/user/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSubmitQuestionnaireSuccessfully() throws Exception {
        // given
        setAuthentication(1L);
        QuestionnaireRequest request = new QuestionnaireRequest(
                30,
                new BigDecimal("75.5"),
                new BigDecimal("180.0"),
                "Intermediate",
                250,
                185,
                5,
                10,
                "Trained for 3 months consistently",
                "Sprained ankle last year",
                "None",
                "Tour de France 2020",
                "Monday, Wednesday, Friday",
                "6-8 AM",
                "Complete a century ride",
                "Gran Fondo",
                LocalDateTime.of(2025, 6, 15, 0, 0));

        // when / then
        mockMvc.perform(post("/api/v1/user/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        then(userProfileService).should().submitQuestionnaire(1L, request);
    }

    @Test
    void shouldReturn400WhenQuestionnaireHasInvalidData() throws Exception {
        // given
        setAuthentication(1L);
        QuestionnaireRequest request = new QuestionnaireRequest(
                200, // Invalid age
                new BigDecimal("75.5"),
                new BigDecimal("180.0"),
                "Intermediate",
                250,
                185,
                5,
                10,
                "Trained for 3 months consistently",
                "Sprained ankle last year",
                "None",
                "Tour de France 2020",
                "Monday, Wednesday, Friday",
                "6-8 AM",
                "Complete a century ride",
                "Gran Fondo",
                LocalDateTime.of(2025, 6, 15, 0, 0));

        // when / then
        mockMvc.perform(post("/api/v1/user/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
