package com.mycyclecoach.feature.trainingplan.dto;

import java.util.List;

public record TrainingPlanDetailResponse(
        Long id,
        Long userId,
        List<PlannedSessionResponse> completedSessions,
        List<PlannedSessionResponse> trainingPlan) {}
