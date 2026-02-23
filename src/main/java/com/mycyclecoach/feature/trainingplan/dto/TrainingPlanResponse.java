package com.mycyclecoach.feature.trainingplan.dto;

import java.time.LocalDate;

public record TrainingPlanResponse(
        Long id, Long userId, LocalDate startDate, LocalDate endDate, String goal, String status) {}
