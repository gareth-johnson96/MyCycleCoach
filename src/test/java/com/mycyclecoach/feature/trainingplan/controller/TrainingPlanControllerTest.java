package com.mycyclecoach.feature.trainingplan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import com.mycyclecoach.feature.trainingplan.service.TrainingPlanService;
import java.time.LocalDate;
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

@WebMvcTest(TrainingPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainingPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrainingPlanService trainingPlanService;

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
        Long userId = 1L;
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void shouldGetCurrentPlanSuccessfully() throws Exception {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        TrainingPlanResponse response = new TrainingPlanResponse(
                1L, userId, today, today.plusWeeks(12), "General Fitness", "ACTIVE");
        given(trainingPlanService.getCurrentPlan(userId)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/v1/training/plan/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.goal").value("General Fitness"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        then(trainingPlanService).should().getCurrentPlan(userId);
    }

    @Test
    void shouldReturn400WhenNoActivePlanFound() throws Exception {
        // given
        Long userId = 1L;
        given(trainingPlanService.getCurrentPlan(userId))
                .willThrow(new IllegalArgumentException("No active plan found for userId: " + userId));

        // when / then
        mockMvc.perform(get("/api/v1/training/plan/current")).andExpect(status().isBadRequest());

        then(trainingPlanService).should().getCurrentPlan(userId);
    }

    @Test
    void shouldGeneratePlanSuccessfullyWithDefaultGoal() throws Exception {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        String defaultGoal = "General Fitness";
        TrainingPlanResponse response =
                new TrainingPlanResponse(1L, userId, today, today.plusWeeks(12), defaultGoal, "ACTIVE");
        given(trainingPlanService.generateBasicPlan(userId, defaultGoal)).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/training/plan/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.goal").value(defaultGoal))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        then(trainingPlanService).should().generateBasicPlan(userId, defaultGoal);
    }

    @Test
    void shouldGeneratePlanSuccessfullyWithCustomGoal() throws Exception {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        String customGoal = "Race Training";
        TrainingPlanResponse response =
                new TrainingPlanResponse(1L, userId, today, today.plusWeeks(12), customGoal, "ACTIVE");
        given(trainingPlanService.generateBasicPlan(userId, customGoal)).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/training/plan/generate").param("goal", customGoal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.goal").value(customGoal))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        then(trainingPlanService).should().generateBasicPlan(userId, customGoal);
    }

    @Test
    void shouldReturn400WhenGeneratePlanFails() throws Exception {
        // given
        Long userId = 1L;
        String goal = "General Fitness";
        given(trainingPlanService.generateBasicPlan(userId, goal))
                .willThrow(new IllegalArgumentException("Failed to generate plan"));

        // when / then
        mockMvc.perform(post("/api/v1/training/plan/generate")).andExpect(status().isBadRequest());

        then(trainingPlanService).should().generateBasicPlan(userId, goal);
    }

    @Test
    void shouldUpdateSessionSuccessfully() throws Exception {
        // given
        Long userId = 1L;
        Long sessionId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");

        // when / then
        mockMvc.perform(put("/api/v1/training/plan/session/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(trainingPlanService).should().markSessionComplete(sessionId, userId, request);
    }

    @Test
    void shouldReturn400WhenUpdatingSessionWithBlankStatus() throws Exception {
        // given
        Long sessionId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("");

        // when / then
        mockMvc.perform(put("/api/v1/training/plan/session/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenSessionNotFound() throws Exception {
        // given
        Long userId = 1L;
        Long sessionId = 999L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");
        doThrow(new IllegalArgumentException("Session not found with id: " + sessionId))
                .when(trainingPlanService)
                .markSessionComplete(sessionId, userId, request);

        // when / then
        mockMvc.perform(put("/api/v1/training/plan/session/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(trainingPlanService).should().markSessionComplete(sessionId, userId, request);
    }

    @Test
    void shouldReturn400WhenUnauthorizedAccessToSession() throws Exception {
        // given
        Long userId = 1L;
        Long sessionId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");
        doThrow(new IllegalArgumentException("Unauthorized access to session: " + sessionId))
                .when(trainingPlanService)
                .markSessionComplete(sessionId, userId, request);

        // when / then
        mockMvc.perform(put("/api/v1/training/plan/session/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(trainingPlanService).should().markSessionComplete(sessionId, userId, request);
    }

    @Test
    void shouldReturn400WhenPlanNotFoundForSession() throws Exception {
        // given
        Long userId = 1L;
        Long sessionId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");
        doThrow(new IllegalArgumentException("Plan not found for session: " + sessionId))
                .when(trainingPlanService)
                .markSessionComplete(sessionId, userId, request);

        // when / then
        mockMvc.perform(put("/api/v1/training/plan/session/{sessionId}", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(trainingPlanService).should().markSessionComplete(sessionId, userId, request);
    }
}
