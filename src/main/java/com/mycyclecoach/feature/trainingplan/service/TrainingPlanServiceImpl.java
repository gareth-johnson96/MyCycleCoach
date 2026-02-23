package com.mycyclecoach.feature.trainingplan.service;

import com.mycyclecoach.feature.trainingplan.domain.PlannedSession;
import com.mycyclecoach.feature.trainingplan.domain.TrainingPlan;
import com.mycyclecoach.feature.trainingplan.dto.CompleteSessionRequest;
import com.mycyclecoach.feature.trainingplan.dto.PlannedSessionResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanDetailResponse;
import com.mycyclecoach.feature.trainingplan.dto.TrainingPlanResponse;
import com.mycyclecoach.feature.trainingplan.repository.PlannedSessionRepository;
import com.mycyclecoach.feature.trainingplan.repository.TrainingPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    @Transactional(readOnly = true)
    public TrainingPlanDetailResponse getPlanWithSessions(Long userId, LocalDate fromDate, LocalDate toDate) {
        TrainingPlan plan = getActivePlan(userId);

        List<PlannedSession> allSessions =
                plannedSessionRepository.findByPlanIdAndScheduledDateBetween(plan.getId(), fromDate, toDate);

        List<PlannedSessionResponse> completedSessions = allSessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .map(this::mapSessionToResponse)
                .toList();

        List<PlannedSessionResponse> plannedSessions = allSessions.stream()
                .filter(s -> "SCHEDULED".equals(s.getStatus()))
                .map(this::mapSessionToResponse)
                .toList();

        return new TrainingPlanDetailResponse(plan.getId(), plan.getUserId(), completedSessions, plannedSessions);
    }

    @Override
    @Transactional
    public List<PlannedSessionResponse> generateSessionsForDateRange(
            Long userId, LocalDate fromDate, LocalDate toDate) {
        TrainingPlan plan = getActivePlan(userId);

        List<PlannedSession> sessions = new ArrayList<>();
        LocalDate currentDate = fromDate;

        while (!currentDate.isAfter(toDate)) {
            PlannedSession session = createSessionForDate(plan.getId(), currentDate);
            PlannedSession saved = plannedSessionRepository.save(session);
            sessions.add(saved);
            currentDate = currentDate.plusDays(2);
        }

        log.info("Generated {} sessions for userId: {} from {} to {}", sessions.size(), userId, fromDate, toDate);
        return sessions.stream().map(this::mapSessionToResponse).toList();
    }

    @Override
    @Transactional
    public PlannedSessionResponse generateSessionForDate(Long userId, LocalDate date) {
        TrainingPlan plan = getActivePlan(userId);

        PlannedSession session = createSessionForDate(plan.getId(), date);
        PlannedSession saved = plannedSessionRepository.save(session);

        log.info("Generated session for userId: {} on date: {}", userId, date);
        return mapSessionToResponse(saved);
    }

    @Override
    @Transactional
    public PlannedSessionResponse generateAlternateSession(Long sessionId, Long userId) {
        PlannedSession originalSession = plannedSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + sessionId));

        TrainingPlan plan = trainingPlanRepository
                .findById(originalSession.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found for session: " + sessionId));

        if (!plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to session: " + sessionId);
        }

        PlannedSession alternateSession = PlannedSession.builder()
                .planId(originalSession.getPlanId())
                .scheduledDate(originalSession.getScheduledDate())
                .type(getAlternateType(originalSession.getType()))
                .distance(getAlternateDistance(originalSession.getDistance()))
                .duration(getAlternateDuration(originalSession.getDuration()))
                .intensity(getAlternateIntensity(originalSession.getIntensity()))
                .tss(getAlternateTss(originalSession.getTss()))
                .elevation(getAlternateElevation(originalSession.getElevation()))
                .targetZone(getAlternateTargetZone(originalSession.getTargetZone()))
                .status("SCHEDULED")
                .build();

        PlannedSession saved = plannedSessionRepository.save(alternateSession);
        log.info(
                "Generated alternate session for original sessionId: {} -> new sessionId: {}",
                sessionId,
                saved.getId());
        return mapSessionToResponse(saved);
    }

    @Override
    public TrainingPlanResponse getCurrentPlan(Long userId) {
        TrainingPlan plan = getActivePlan(userId);
        return mapToResponse(plan);
    }

    private TrainingPlan getActivePlan(Long userId) {
        List<TrainingPlan> plans = trainingPlanRepository.findAll();
        return plans.stream()
                .filter(p -> p.getUserId().equals(userId) && "ACTIVE".equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active plan found for userId: " + userId));
    }

    private PlannedSession createSessionForDate(Long planId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        String type = dayOfWeek <= 2 ? "EASY" : dayOfWeek <= 4 ? "TEMPO" : "LONG";
        BigDecimal distance = dayOfWeek <= 2
                ? BigDecimal.valueOf(10)
                : dayOfWeek <= 4 ? BigDecimal.valueOf(15) : BigDecimal.valueOf(20);
        Integer duration = dayOfWeek <= 2 ? 60 : dayOfWeek <= 4 ? 75 : 120;
        String intensity = dayOfWeek <= 2 ? "LOW" : dayOfWeek <= 4 ? "MEDIUM" : "HIGH";
        Integer tss = dayOfWeek <= 2 ? 50 : dayOfWeek <= 4 ? 75 : 100;
        Integer elevation = dayOfWeek <= 2 ? 100 : dayOfWeek <= 4 ? 200 : 400;
        String targetZone = dayOfWeek <= 2 ? "ZONE2" : dayOfWeek <= 4 ? "ZONE3" : "ZONE4";

        return PlannedSession.builder()
                .planId(planId)
                .scheduledDate(date)
                .type(type)
                .distance(distance)
                .duration(duration)
                .intensity(intensity)
                .tss(tss)
                .elevation(elevation)
                .targetZone(targetZone)
                .status("SCHEDULED")
                .build();
    }

    private String getAlternateType(String original) {
        return switch (original) {
            case "EASY" -> "RECOVERY";
            case "TEMPO" -> "INTERVALS";
            case "LONG" -> "ENDURANCE";
            default -> "EASY";
        };
    }

    private BigDecimal getAlternateDistance(BigDecimal original) {
        if (original == null) return BigDecimal.valueOf(10);
        return original.multiply(BigDecimal.valueOf(0.8));
    }

    private Integer getAlternateDuration(Integer original) {
        if (original == null) return 60;
        return (int) (original * 0.8);
    }

    private String getAlternateIntensity(String original) {
        return switch (original) {
            case "LOW" -> "MEDIUM";
            case "MEDIUM" -> "LOW";
            case "HIGH" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private Integer getAlternateTss(Integer original) {
        if (original == null) return 40;
        return (int) (original * 0.8);
    }

    private Integer getAlternateElevation(Integer original) {
        if (original == null) return 50;
        return (int) (original * 0.7);
    }

    private String getAlternateTargetZone(String original) {
        if (original == null) return "ZONE2";
        return switch (original) {
            case "ZONE2" -> "ZONE1";
            case "ZONE3" -> "ZONE2";
            case "ZONE4" -> "ZONE3";
            case "ZONE5" -> "ZONE4";
            default -> "ZONE2";
        };
    }

    private PlannedSessionResponse mapSessionToResponse(PlannedSession session) {
        return new PlannedSessionResponse(
                session.getId(),
                session.getPlanId(),
                session.getScheduledDate(),
                session.getType(),
                session.getDistance(),
                session.getDuration(),
                session.getIntensity(),
                session.getStatus(),
                session.getTss(),
                session.getElevation(),
                session.getTargetZone());
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
                PlannedSession session = createSessionForDate(savedPlan.getId(), currentDate);
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

    @Override
    @Transactional
    public TrainingPlanDetailResponse generateRandomTestData(Long userId, LocalDate fromDate, LocalDate toDate) {
        TrainingPlan plan = getActivePlan(userId);
        Random random = new Random();

        List<PlannedSession> completedSessions = new ArrayList<>();
        List<PlannedSession> plannedSessions = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate currentDate = fromDate;

        while (!currentDate.isAfter(toDate)) {
            // Randomly decide whether to create a session for this date (60% chance)
            if (random.nextDouble() < 0.6) {
                PlannedSession session = createRandomSession(plan.getId(), currentDate, random);

                // If the date is in the past, mark it as completed (80% chance) or skipped (20% chance)
                if (currentDate.isBefore(today)) {
                    if (random.nextDouble() < 0.8) {
                        session.setStatus("COMPLETED");
                        session.setCompletedAt(
                                currentDate.atTime(random.nextInt(20) + 4, random.nextInt(60)));
                        completedSessions.add(session);
                    } else {
                        session.setStatus("SKIPPED");
                    }
                } else {
                    // Future sessions are scheduled
                    session.setStatus("SCHEDULED");
                    plannedSessions.add(session);
                }

                plannedSessionRepository.save(session);
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info(
                "Generated random test data for userId: {} - {} completed, {} planned sessions",
                userId,
                completedSessions.size(),
                plannedSessions.size());

        List<PlannedSessionResponse> completedResponses =
                completedSessions.stream().map(this::mapSessionToResponse).toList();
        List<PlannedSessionResponse> plannedResponses =
                plannedSessions.stream().map(this::mapSessionToResponse).toList();

        return new TrainingPlanDetailResponse(plan.getId(), plan.getUserId(), completedResponses, plannedResponses);
    }

    private PlannedSession createRandomSession(Long planId, LocalDate date, Random random) {
        String[] types = {"EASY", "TEMPO", "LONG", "RECOVERY", "INTERVALS", "ENDURANCE"};
        String[] intensities = {"LOW", "MEDIUM", "HIGH"};
        String[] targetZones = {"ZONE1", "ZONE2", "ZONE3", "ZONE4", "ZONE5"};

        String type = types[random.nextInt(types.length)];
        String intensity = intensities[random.nextInt(intensities.length)];
        String targetZone = targetZones[random.nextInt(targetZones.length)];

        // Randomize values with realistic ranges
        BigDecimal distance = BigDecimal.valueOf(5 + random.nextInt(95)); // 5-100 km
        Integer duration = 30 + random.nextInt(210); // 30-240 minutes
        Integer tss = 20 + random.nextInt(180); // 20-200 TSS
        Integer elevation = 0 + random.nextInt(2000); // 0-2000 m

        return PlannedSession.builder()
                .planId(planId)
                .scheduledDate(date)
                .type(type)
                .distance(distance)
                .duration(duration)
                .intensity(intensity)
                .tss(tss)
                .elevation(elevation)
                .targetZone(targetZone)
                .status("SCHEDULED")
                .build();
    }
}
