package com.mycyclecoach.feature.trainingplan.service;

import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.PlannedSessionResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanDetailResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import java.time.LocalDate;
import java.util.List;

public interface TrainingPlanService {

    TrainingPlanResponse getCurrentPlan(Long userId);

    TrainingPlanDetailResponse getPlanWithSessions(Long userId, LocalDate fromDate, LocalDate toDate);

    TrainingPlanResponse generateBasicPlan(Long userId, String goal);

    List<PlannedSessionResponse> generateSessionsForDateRange(Long userId, LocalDate fromDate, LocalDate toDate);

    PlannedSessionResponse generateSessionForDate(Long userId, LocalDate date);

    void markSessionComplete(Long sessionId, Long userId, CompleteSessionRequest request);

    PlannedSessionResponse generateAlternateSession(Long sessionId, Long userId);
}
