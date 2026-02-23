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
        @Min(value = 50, message = "Height must be at least 50")
                @Max(value = 300, message = "Height must be less than 300")
                BigDecimal height,
        String experienceLevel,
        @Min(value = 0, message = "FTP must be at least 0") @Max(value = 600, message = "FTP must be less than 600")
                Integer currentFtp,
        @Min(value = 50, message = "Max HR must be at least 50")
                @Max(value = 250, message = "Max HR must be less than 250")
                Integer maxHr) {}
