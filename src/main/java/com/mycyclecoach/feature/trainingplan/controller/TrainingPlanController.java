package com.mycyclecoach.feature.trainingplan.controller;

import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import com.mycyclecoach.feature.trainingplan.service.TrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
