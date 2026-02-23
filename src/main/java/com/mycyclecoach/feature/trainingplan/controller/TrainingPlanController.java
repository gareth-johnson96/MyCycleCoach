package com.mycyclecoach.feature.trainingplan.controller;

import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.PlannedSessionResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanDetailResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import com.mycyclecoach.feature.trainingplan.service.TrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training/plan")
@RequiredArgsConstructor
@Tag(name = "Training Plan", description = "Training plan generation and session management")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/current")
    @Operation(
            summary = "Get current training plan",
            description = "Retrieve the active training plan for the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No active plan found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TrainingPlanResponse> getCurrentPlan() {
        Long userId = getCurrentUserId();
        TrainingPlanResponse plan = trainingPlanService.getCurrentPlan(userId);
        return ResponseEntity.ok(plan);
    }

    @GetMapping
    @Operation(
            summary = "Get training plan with sessions",
            description =
                    "Retrieve the active training plan with completed and planned sessions filtered by date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan and sessions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No active plan found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TrainingPlanDetailResponse> getPlanWithSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Long userId = getCurrentUserId();
        TrainingPlanDetailResponse plan = trainingPlanService.getPlanWithSessions(userId, fromDate, toDate);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate training plan", description = "Generate a new training plan for the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TrainingPlanResponse> generatePlan(
            @RequestParam(required = false, defaultValue = "General Fitness") String goal) {
        Long userId = getCurrentUserId();
        TrainingPlanResponse plan = trainingPlanService.generateBasicPlan(userId, goal);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/generate/sessions")
    @Operation(
            summary = "Generate sessions for date range",
            description = "Generate training sessions for a specific date range or single date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessions generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PlannedSessionResponse>> generateSessionsForDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Long userId = getCurrentUserId();
        if (toDate == null) {
            PlannedSessionResponse session = trainingPlanService.generateSessionForDate(userId, fromDate);
            return ResponseEntity.ok(List.of(session));
        }
        List<PlannedSessionResponse> sessions =
                trainingPlanService.generateSessionsForDateRange(userId, fromDate, toDate);
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/session/{sessionId}")
    @Operation(summary = "Update planned session", description = "Mark a planned session as completed or skipped")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> updateSession(
            @PathVariable Long sessionId, @Valid @RequestBody CompleteSessionRequest request) {
        Long userId = getCurrentUserId();
        trainingPlanService.markSessionComplete(sessionId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/alternate")
    @Operation(
            summary = "Generate alternate session",
            description = "Generate an alternate session based on an existing session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alternate session generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PlannedSessionResponse> generateAlternateSession(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        PlannedSessionResponse alternateSession = trainingPlanService.generateAlternateSession(sessionId, userId);
        return ResponseEntity.ok(alternateSession);
    }

    @PostMapping("/generate/test-data")
    @Operation(
            summary = "Generate random test data",
            description =
                    "Generate random completed rides and upcoming sessions for testing the front end. Creates realistic test data with random session types, distances, and intensities.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Test data generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "No active plan found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TrainingPlanDetailResponse> generateTestData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Long userId = getCurrentUserId();
        TrainingPlanDetailResponse response = trainingPlanService.generateRandomTestData(userId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }
}
