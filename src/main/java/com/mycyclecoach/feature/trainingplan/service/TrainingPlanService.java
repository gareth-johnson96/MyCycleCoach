package com.mycyclecoach.feature.trainingplan.service;

import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;

public interface TrainingPlanService {

    TrainingPlanResponse getCurrentPlan(Long userId);

    TrainingPlanResponse generateBasicPlan(Long userId, String goal);

    void markSessionComplete(Long sessionId, Long userId, CompleteSessionRequest request);
}
