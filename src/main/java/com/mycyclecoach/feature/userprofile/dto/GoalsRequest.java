package com.mycyclecoach.feature.userprofile.dto;

import jakarta.validation.constraints.NotBlank;

public record GoalsRequest(@NotBlank(message = "Goals are required") String goals) {}
