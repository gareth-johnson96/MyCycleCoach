package com.mycyclecoach.feature.userprofile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateProfileRequest(
        @Min(value = 1, message = "Age must be at least 1") @Max(value = 150, message = "Age must be less than 150")
                Integer age,
        @Min(value = 1, message = "Weight must be at least 1")
                @Max(value = 500, message = "Weight must be less than 500")
                BigDecimal weight,
        String experienceLevel) {}
