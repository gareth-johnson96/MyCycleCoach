package com.mycyclecoach.feature.trainingplan.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteSessionRequest(@NotBlank(message = "Status is required") String status) {}
