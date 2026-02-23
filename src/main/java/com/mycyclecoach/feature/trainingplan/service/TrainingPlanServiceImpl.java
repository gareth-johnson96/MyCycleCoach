package com.mycyclecoach.feature.trainingplan.service;

import com.mycyclecoach.feature.trainingplan.domain.PlannedSession;
import com.mycyclecoach.feature.trainingplan.domain.TrainingPlan;
import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import com.mycyclecoach.feature.trainingplan.repository.PlannedSessionRepository;
import com.mycyclecoach.feature.trainingplan.repository.TrainingPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedSessionRepository plannedSessionRepository;

    @Override
    public TrainingPlanResponse getCurrentPlan(Long userId) {
        // For MVP, just fetch the first active plan for the user
        List<TrainingPlan> plans = trainingPlanRepository.findAll();
        TrainingPlan plan = plans.stream()
                .filter(p -> p.getUserId().equals(userId) && "ACTIVE".equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active plan found for userId: " + userId));

        return mapToResponse(plan);
    }

    @Override
    @Transactional
    public TrainingPlanResponse generateBasicPlan(Long userId, String goal) {
        // Delete any existing active plans
        List<TrainingPlan> existingPlans = trainingPlanRepository.findAll();
        existingPlans.stream()
                .filter(p -> p.getUserId().equals(userId) && "ACTIVE".equals(p.getStatus()))
                .forEach(p -> p.setStatus("COMPLETED"));

        // Create a new basic 12-week training plan
        LocalDate today = LocalDate.now();
        TrainingPlan plan = TrainingPlan.builder()
                .userId(userId)
                .startDate(today)
                .endDate(today.plusWeeks(12))
                .goal(goal)
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        TrainingPlan savedPlan = trainingPlanRepository.save(plan);

        // Generate basic sessions (3 per week for 12 weeks = 36 sessions)
        LocalDate currentDate = today;
        for (int week = 0; week < 12; week++) {
            for (int day = 0; day < 3; day++) {
                PlannedSession session = PlannedSession.builder()
                        .planId(savedPlan.getId())
                        .scheduledDate(currentDate)
                        .type(day == 0 ? "EASY" : day == 1 ? "TEMPO" : "LONG")
                        .distance(
                                day == 0
                                        ? BigDecimal.valueOf(10)
                                        : day == 1 ? BigDecimal.valueOf(15) : BigDecimal.valueOf(20))
                        .duration(day == 0 ? 60 : day == 1 ? 75 : 120)
                        .intensity(day == 0 ? "LOW" : day == 1 ? "MEDIUM" : "HIGH")
                        .status("SCHEDULED")
                        .build();
                plannedSessionRepository.save(session);
                currentDate = currentDate.plusDays(2);
            }
        }

        log.info("Basic training plan generated for userId: {} with goal: {}", userId, goal);
        return mapToResponse(savedPlan);
    }

    @Override
    @Transactional
    public void markSessionComplete(Long sessionId, Long userId, CompleteSessionRequest request) {
        PlannedSession session = plannedSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + sessionId));

        // Verify the session belongs to a plan for this user
        TrainingPlan plan = trainingPlanRepository
                .findById(session.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found for session: " + sessionId));

        if (!plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to session: " + sessionId);
        }

        session.setStatus(request.status());
        if ("COMPLETED".equals(request.status())) {
            session.setCompletedAt(LocalDateTime.now());
        }

        plannedSessionRepository.save(session);
        log.info("Training session marked as {} for sessionId: {}", request.status(), sessionId);
    }

    private TrainingPlanResponse mapToResponse(TrainingPlan plan) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getUserId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getGoal(),
                plan.getStatus());
    }
}
