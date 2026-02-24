package com.mycyclecoach.feature.strava.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycyclecoach.config.SecurityConfig;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.strava.dto.RideResponse;
import com.mycyclecoach.feature.strava.dto.StravaConnectionResponse;
import com.mycyclecoach.feature.strava.service.StravaAuthService;
import com.mycyclecoach.feature.strava.service.StravaSyncService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StravaController.class)
@Import(SecurityConfig.class)
class StravaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StravaAuthService stravaAuthService;

    @MockBean
    private StravaSyncService stravaSyncService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void shouldReturnAuthorizationUrlWhenRequested() throws Exception {
        // given
        String authUrl = "https://www.strava.com/oauth/authorize?client_id=123";
        given(stravaAuthService.generateAuthorizationUrl()).willReturn(authUrl);

        // when / then
        mockMvc.perform(get("/api/v1/strava/authorize"))
                .andExpect(status().isOk())
                .andExpect(content().string(authUrl));
    }

    @Test
    @WithMockUser
    void shouldReturnConnectionStatusWhenConnectionExists() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        StravaConnectionResponse response = new StravaConnectionResponse(1L, userId, 12345L, true, LocalDateTime.now());

        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
        given(stravaAuthService.getConnectionStatus(userId)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/v1/strava/connection").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.stravaAthleteId").value(12345L))
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    @WithMockUser
    void shouldDisconnectStravaWhenRequested() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
        willDoNothing().given(stravaAuthService).disconnect(userId);

        // when / then
        mockMvc.perform(delete("/api/v1/strava/connection").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        then(stravaAuthService).should().disconnect(userId);
    }

    @Test
    @WithMockUser
    void shouldReturnRidesForCurrentUser() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        RideResponse ride = new RideResponse(
                1L,
                12345L,
                "Morning Ride",
                new BigDecimal("25.5"),
                3600,
                3700,
                new BigDecimal("250"),
                LocalDateTime.now(),
                new BigDecimal("7.08"),
                new BigDecimal("12.5"),
                null,
                null,
                null,
                "Ride",
                null,
                "Ride",
                null);

        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
        given(stravaSyncService.getUserRides(userId)).willReturn(List.of(ride));

        // when / then
        mockMvc.perform(get("/api/v1/strava/rides").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning Ride"))
                .andExpect(jsonPath("$[0].stravaActivityId").value(12345L));
    }

    @Test
    @WithMockUser
    void shouldAcceptSyncRequestForCurrentUser() throws Exception {
        // given
        Long userId = 1L;
        String token = "test-token";
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
        willDoNothing().given(stravaSyncService).syncRidesForUser(userId);

        // when / then
        mockMvc.perform(post("/api/v1/strava/sync").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        then(stravaSyncService).should().syncRidesForUser(userId);
    }
}
