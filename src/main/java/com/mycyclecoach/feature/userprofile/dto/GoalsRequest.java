package com.mycyclecoach.feature.userprofile.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record GoalsRequest(
        @NotBlank(message = "Goals are required") String goals, String targetEvent, LocalDateTime targetEventDate) {}
