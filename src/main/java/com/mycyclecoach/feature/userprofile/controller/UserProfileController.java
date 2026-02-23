package com.mycyclecoach.feature.userprofile.controller;

import com.mycyclecoach.feature.userprofile.dto.*;
import com.mycyclecoach.feature.userprofile.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile and training background management")
public class UserProfileController {

    private final UserProfileService userProfileService;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Retrieve the current user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Profile not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProfileResponse> getProfile() {
        Long userId = getCurrentUserId();
        ProfileResponse profile = userProfileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/background")
    @Operation(summary = "Get training background", description = "Retrieve the current user's training background")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Background retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Background not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BackgroundResponse> getBackground() {
        Long userId = getCurrentUserId();
        BackgroundResponse background = userProfileService.getBackground(userId);
        return ResponseEntity.ok(background);
    }

    @GetMapping("/goals")
    @Operation(summary = "Get training goals", description = "Retrieve the current user's training goals")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Goals retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Goals not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GoalsResponse> getGoals() {
        Long userId = getCurrentUserId();
        GoalsResponse goals = userProfileService.getGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/complete-profile")
    @Operation(
            summary = "Get complete profile",
            description = "Retrieve the current user's complete profile including background and goals")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Complete profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Profile data not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CompleteProfileResponse> getCompleteProfile() {
        Long userId = getCurrentUserId();
        CompleteProfileResponse completeProfile = userProfileService.getCompleteProfile(userId);
        return ResponseEntity.ok(completeProfile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the current user's profile information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Profile not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/background")
    @Operation(
            summary = "Save training background",
            description = "Submit the user's training background and experience")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Background saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> saveBackground(@Valid @RequestBody BackgroundRequest request) {
        Long userId = getCurrentUserId();
        userProfileService.saveBackground(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/goals")
    @Operation(summary = "Update training goals", description = "Update the user's training goals")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Goals updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> updateGoals(@Valid @RequestBody GoalsRequest request) {
        Long userId = getCurrentUserId();
        userProfileService.updateGoals(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/questionnaire")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit user questionnaire",
            description =
                    "Submit a comprehensive questionnaire upon account creation that populates the user's profile,"
                            + " training background, and goals")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Questionnaire submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> submitQuestionnaire(@Valid @RequestBody QuestionnaireRequest request) {
        Long userId = getCurrentUserId();
        userProfileService.submitQuestionnaire(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
