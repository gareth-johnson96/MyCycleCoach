package com.mycyclecoach.feature.userprofile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuestionnaireRequest(
        // User Profile fields
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
                Integer maxHr,
        // Training Background fields
        Integer yearsTraining,
        Integer weeklyVolume,
        String trainingHistory,
        String injuryHistory,
        String recentInjuries,
        String priorEvents,
        String dailyAvailability,
        String weeklyTrainingTimes,
        // Training Goals fields
        @NotBlank(message = "Goals are required") String goals,
        String targetEvent,
        LocalDateTime targetEventDate) {}
