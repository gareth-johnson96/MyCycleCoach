package com.mycyclecoach.feature.gpxanalysis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.security.JwtAuthenticationFilter;
import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.gpxanalysis.domain.ClimbResponse;
import com.mycyclecoach.feature.gpxanalysis.domain.GpxAnalysisResponse;
import com.mycyclecoach.feature.gpxanalysis.service.GpxAnalysisService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GpxAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class GpxAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GpxAnalysisService gpxAnalysisService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void shouldReturn201WhenGpxFileIsUploaded() throws Exception {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.gpx", "application/gpx+xml", "gpx content".getBytes());

        ClimbResponse climbResponse = new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 10);
        GpxAnalysisResponse response = new GpxAnalysisResponse(
                1L, "test.gpx", 1, List.of(climbResponse), 1.5, 5.0, LocalDateTime.now());

        given(gpxAnalysisService.analyzeGpxFile(any(), eq(100L))).willReturn(response);

        // when / then
        mockMvc.perform(multipart("/api/v1/gpx/upload").file(file).param("userId", "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gpxFileId").value(1L))
                .andExpect(jsonPath("$.filename").value("test.gpx"))
                .andExpect(jsonPath("$.climbCount").value(1))
                .andExpect(jsonPath("$.climbs[0].distanceMeters").value(200.0))
                .andExpect(jsonPath("$.climbs[0].elevationGainMeters").value(40.0));
    }

    @Test
    void shouldReturn200WhenGettingGpxAnalysisByIdSuccessfully() throws Exception {
        // given
        ClimbResponse climbResponse = new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 10);
        GpxAnalysisResponse response = new GpxAnalysisResponse(
                1L, "test.gpx", 1, List.of(climbResponse), 1.5, 5.0, LocalDateTime.now());

        given(gpxAnalysisService.getGpxAnalysis(1L)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/v1/gpx/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gpxFileId").value(1L))
                .andExpect(jsonPath("$.filename").value("test.gpx"))
                .andExpect(jsonPath("$.climbCount").value(1));
    }

    @Test
    void shouldReturn200WhenAnalyzingGpxByFilenameSuccessfully() throws Exception {
        // given
        String filename = "test_ride.gpx";
        ClimbResponse climbResponse = new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 10);
        GpxAnalysisResponse response = new GpxAnalysisResponse(
                1L, filename, 1, List.of(climbResponse), 10.5, 28.3, LocalDateTime.now());

        given(gpxAnalysisService.analyzeByFilename(filename)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/v1/gpx/analyze/" + filename))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gpxFileId").value(1L))
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.climbCount").value(1))
                .andExpect(jsonPath("$.totalDistanceKm").value(10.5))
                .andExpect(jsonPath("$.estimatedRideTimeMinutes").value(28.3));
    }
}
