package com.mycyclecoach.feature.trainingplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceImplTest {

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @Mock
    private PlannedSessionRepository plannedSessionRepository;

    @InjectMocks
    private TrainingPlanServiceImpl trainingPlanService;

    @Test
    void shouldGetCurrentPlanSuccessfully() {
        // given
        Long userId = 1L;
        TrainingPlan activePlan = TrainingPlan.builder()
                .id(1L)
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        List<TrainingPlan> plans = Collections.singletonList(activePlan);
        given(trainingPlanRepository.findAll()).willReturn(plans);

        // when
        TrainingPlanResponse response = trainingPlanService.getCurrentPlan(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.goal()).isEqualTo("General Fitness");
        assertThat(response.status()).isEqualTo("ACTIVE");

        then(trainingPlanRepository).should().findAll();
    }

    @Test
    void shouldThrowExceptionWhenNoActivePlanFound() {
        // given
        Long userId = 1L;
        TrainingPlan inactivePlan = TrainingPlan.builder()
                .id(1L)
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("COMPLETED")
                .generatedAt(LocalDateTime.now())
                .build();

        List<TrainingPlan> plans = Collections.singletonList(inactivePlan);
        given(trainingPlanRepository.findAll()).willReturn(plans);

        // when / then
        assertThatThrownBy(() -> trainingPlanService.getCurrentPlan(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active plan found for userId: " + userId);

        then(trainingPlanRepository).should().findAll();
    }

    @Test
    void shouldThrowExceptionWhenNoPlansExistForUser() {
        // given
        Long userId = 1L;
        TrainingPlan otherUserPlan = TrainingPlan.builder()
                .id(1L)
                .userId(2L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        List<TrainingPlan> plans = Collections.singletonList(otherUserPlan);
        given(trainingPlanRepository.findAll()).willReturn(plans);

        // when / then
        assertThatThrownBy(() -> trainingPlanService.getCurrentPlan(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active plan found for userId: " + userId);

        then(trainingPlanRepository).should().findAll();
    }

    @Test
    void shouldGenerateBasicPlanSuccessfully() {
        // given
        Long userId = 1L;
        String goal = "General Fitness";
        LocalDate today = LocalDate.now();

        TrainingPlan savedPlan = TrainingPlan.builder()
                .id(1L)
                .userId(userId)
                .startDate(today)
                .endDate(today.plusWeeks(12))
                .goal(goal)
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(Collections.emptyList());
        given(trainingPlanRepository.save(any(TrainingPlan.class))).willReturn(savedPlan);
        given(plannedSessionRepository.save(any(PlannedSession.class))).willReturn(new PlannedSession());

        // when
        TrainingPlanResponse response = trainingPlanService.generateBasicPlan(userId, goal);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.goal()).isEqualTo(goal);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.startDate()).isEqualTo(today);
        assertThat(response.endDate()).isEqualTo(today.plusWeeks(12));

        then(trainingPlanRepository).should().findAll();
        then(trainingPlanRepository).should().save(any(TrainingPlan.class));
        then(plannedSessionRepository).should(times(36)).save(any(PlannedSession.class));
    }

    @Test
    void shouldMarkExistingActivePlanAsCompletedWhenGeneratingNewPlan() {
        // given
        Long userId = 1L;
        String goal = "Race Training";
        LocalDate today = LocalDate.now();

        TrainingPlan existingPlan = TrainingPlan.builder()
                .id(1L)
                .userId(userId)
                .startDate(LocalDate.now().minusWeeks(5))
                .endDate(LocalDate.now().plusWeeks(7))
                .goal("Old Goal")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        TrainingPlan newPlan = TrainingPlan.builder()
                .id(2L)
                .userId(userId)
                .startDate(today)
                .endDate(today.plusWeeks(12))
                .goal(goal)
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(Collections.singletonList(existingPlan));
        given(trainingPlanRepository.save(any(TrainingPlan.class))).willReturn(newPlan);
        given(plannedSessionRepository.save(any(PlannedSession.class))).willReturn(new PlannedSession());

        // when
        TrainingPlanResponse response = trainingPlanService.generateBasicPlan(userId, goal);

        // then
        assertThat(response).isNotNull();
        assertThat(response.goal()).isEqualTo(goal);
        assertThat(existingPlan.getStatus()).isEqualTo("COMPLETED");

        then(trainingPlanRepository).should().findAll();
        then(trainingPlanRepository).should().save(any(TrainingPlan.class));
    }

    @Test
    void shouldNotMarkOtherUsersPlansAsCompletedWhenGeneratingNewPlan() {
        // given
        Long userId = 1L;
        String goal = "Race Training";
        LocalDate today = LocalDate.now();

        TrainingPlan otherUserPlan = TrainingPlan.builder()
                .id(1L)
                .userId(2L)
                .startDate(LocalDate.now().minusWeeks(5))
                .endDate(LocalDate.now().plusWeeks(7))
                .goal("Other User Goal")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        TrainingPlan newPlan = TrainingPlan.builder()
                .id(2L)
                .userId(userId)
                .startDate(today)
                .endDate(today.plusWeeks(12))
                .goal(goal)
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(Collections.singletonList(otherUserPlan));
        given(trainingPlanRepository.save(any(TrainingPlan.class))).willReturn(newPlan);
        given(plannedSessionRepository.save(any(PlannedSession.class))).willReturn(new PlannedSession());

        // when
        trainingPlanService.generateBasicPlan(userId, goal);

        // then
        assertThat(otherUserPlan.getStatus()).isEqualTo("ACTIVE");

        then(trainingPlanRepository).should().findAll();
        then(trainingPlanRepository).should().save(any(TrainingPlan.class));
    }

    @Test
    void shouldMarkSessionCompleteSuccessfully() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long planId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");

        PlannedSession session = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .status("SCHEDULED")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.of(plan));
        given(plannedSessionRepository.save(any(PlannedSession.class))).willReturn(session);

        // when
        trainingPlanService.markSessionComplete(sessionId, userId, request);

        // then
        assertThat(session.getStatus()).isEqualTo("COMPLETED");
        assertThat(session.getCompletedAt()).isNotNull();

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should().save(session);
    }

    @Test
    void shouldMarkSessionAsSkippedWithoutCompletedAt() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long planId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("SKIPPED");

        PlannedSession session = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .status("SCHEDULED")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.of(plan));
        given(plannedSessionRepository.save(any(PlannedSession.class))).willReturn(session);

        // when
        trainingPlanService.markSessionComplete(sessionId, userId, request);

        // then
        assertThat(session.getStatus()).isEqualTo("SKIPPED");
        assertThat(session.getCompletedAt()).isNull();

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should().save(session);
    }

    @Test
    void shouldThrowExceptionWhenSessionNotFound() {
        // given
        Long sessionId = 999L;
        Long userId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> trainingPlanService.markSessionComplete(sessionId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found with id: " + sessionId);

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should(never()).findById(anyLong());
        then(plannedSessionRepository).should(never()).save(any(PlannedSession.class));
    }

    @Test
    void shouldThrowExceptionWhenPlanNotFoundForSession() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long planId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");

        PlannedSession session = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .status("SCHEDULED")
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> trainingPlanService.markSessionComplete(sessionId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan not found for session: " + sessionId);

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should(never()).save(any(PlannedSession.class));
    }

    @Test
    void shouldThrowExceptionWhenUnauthorizedUserTriesToCompleteSession() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long otherUserId = 2L;
        Long planId = 1L;
        CompleteSessionRequest request = new CompleteSessionRequest("COMPLETED");

        PlannedSession session = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .status("SCHEDULED")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .id(planId)
                .userId(otherUserId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.of(plan));

        // when / then
        assertThatThrownBy(() -> trainingPlanService.markSessionComplete(sessionId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized access to session: " + sessionId);

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should(never()).save(any(PlannedSession.class));
    }

    @Test
    void shouldGetPlanWithSessionsFilteredByDateRange() {
        // given
        Long userId = 1L;
        Long planId = 1L;
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(10);

        TrainingPlan activePlan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(fromDate)
                .endDate(toDate.plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        PlannedSession completedSession = PlannedSession.builder()
                .id(1L)
                .planId(planId)
                .scheduledDate(fromDate.plusDays(1))
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .tss(50)
                .elevation(100)
                .targetZone("ZONE2")
                .status("COMPLETED")
                .build();

        PlannedSession scheduledSession = PlannedSession.builder()
                .id(2L)
                .planId(planId)
                .scheduledDate(fromDate.plusDays(3))
                .type("TEMPO")
                .distance(BigDecimal.valueOf(15))
                .duration(75)
                .intensity("MEDIUM")
                .tss(75)
                .elevation(200)
                .targetZone("ZONE3")
                .status("SCHEDULED")
                .build();

        given(trainingPlanRepository.findAll()).willReturn(List.of(activePlan));
        given(plannedSessionRepository.findByPlanIdAndScheduledDateBetween(planId, fromDate, toDate))
                .willReturn(List.of(completedSession, scheduledSession));

        // when
        var response = trainingPlanService.getPlanWithSessions(userId, fromDate, toDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(planId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.completedSessions()).hasSize(1);
        assertThat(response.trainingPlan()).hasSize(1);
        assertThat(response.completedSessions().get(0).status()).isEqualTo("COMPLETED");
        assertThat(response.trainingPlan().get(0).status()).isEqualTo("SCHEDULED");
    }

    @Test
    void shouldGenerateSessionsForDateRange() {
        // given
        Long userId = 1L;
        Long planId = 1L;
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(4);

        TrainingPlan activePlan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(fromDate)
                .endDate(toDate.plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(List.of(activePlan));
        given(plannedSessionRepository.save(any(PlannedSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = trainingPlanService.generateSessionsForDateRange(userId, fromDate, toDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response.size()).isGreaterThan(0);
        then(plannedSessionRepository).should(times(response.size())).save(any(PlannedSession.class));
    }

    @Test
    void shouldGenerateSessionForSingleDate() {
        // given
        Long userId = 1L;
        Long planId = 1L;
        LocalDate date = LocalDate.now();

        TrainingPlan activePlan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(date)
                .endDate(date.plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(List.of(activePlan));
        given(plannedSessionRepository.save(any(PlannedSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = trainingPlanService.generateSessionForDate(userId, date);

        // then
        assertThat(response).isNotNull();
        assertThat(response.scheduledDate()).isEqualTo(date);
        then(plannedSessionRepository).should().save(any(PlannedSession.class));
    }

    @Test
    void shouldGenerateAlternateSession() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long planId = 1L;

        PlannedSession originalSession = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .tss(50)
                .elevation(100)
                .targetZone("ZONE2")
                .status("SCHEDULED")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(originalSession));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.of(plan));
        given(plannedSessionRepository.save(any(PlannedSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = trainingPlanService.generateAlternateSession(sessionId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo("RECOVERY");
        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should().save(any(PlannedSession.class));
    }

    @Test
    void shouldThrowExceptionWhenGeneratingAlternateForNonExistentSession() {
        // given
        Long sessionId = 999L;
        Long userId = 1L;

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> trainingPlanService.generateAlternateSession(sessionId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found with id: " + sessionId);

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should(never()).findById(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenUnauthorizedUserTriesToGenerateAlternate() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        Long otherUserId = 2L;
        Long planId = 1L;

        PlannedSession originalSession = PlannedSession.builder()
                .id(sessionId)
                .planId(planId)
                .scheduledDate(LocalDate.now())
                .type("EASY")
                .distance(BigDecimal.valueOf(10))
                .duration(60)
                .intensity("LOW")
                .status("SCHEDULED")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .id(planId)
                .userId(otherUserId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(plannedSessionRepository.findById(sessionId)).willReturn(Optional.of(originalSession));
        given(trainingPlanRepository.findById(planId)).willReturn(Optional.of(plan));

        // when / then
        assertThatThrownBy(() -> trainingPlanService.generateAlternateSession(sessionId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized access to session: " + sessionId);

        then(plannedSessionRepository).should().findById(sessionId);
        then(trainingPlanRepository).should().findById(planId);
        then(plannedSessionRepository).should(never()).save(any(PlannedSession.class));
    }

    @Test
    void shouldGenerateRandomTestDataForDateRange() {
        // given
        Long userId = 1L;
        Long planId = 1L;
        LocalDate fromDate = LocalDate.now().minusDays(10);
        LocalDate toDate = LocalDate.now().plusDays(10);

        TrainingPlan activePlan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(fromDate)
                .endDate(toDate.plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(List.of(activePlan));
        given(plannedSessionRepository.save(any(PlannedSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TrainingPlanDetailResponse response = trainingPlanService.generateRandomTestData(userId, fromDate, toDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(planId);
        assertThat(response.userId()).isEqualTo(userId);

        // Verify that completed sessions are in the past
        response.completedSessions().forEach(session -> {
            assertThat(session.scheduledDate()).isBefore(LocalDate.now());
            assertThat(session.status()).isEqualTo("COMPLETED");
        });

        // Verify that planned sessions are today or in the future
        response.trainingPlan().forEach(session -> {
            assertThat(session.scheduledDate()).isAfterOrEqualTo(LocalDate.now());
            assertThat(session.status()).isEqualTo("SCHEDULED");
        });

        // Verify session properties are randomized and realistic
        List<PlannedSessionResponse> allSessions = new ArrayList<>();
        allSessions.addAll(response.completedSessions());
        allSessions.addAll(response.trainingPlan());

        if (!allSessions.isEmpty()) {
            allSessions.forEach(session -> {
                assertThat(session.type()).isNotNull();
                assertThat(session.distance()).isNotNull();
                assertThat(session.distance()).isGreaterThanOrEqualTo(BigDecimal.valueOf(5));
                assertThat(session.distance()).isLessThanOrEqualTo(BigDecimal.valueOf(100));
                assertThat(session.duration()).isNotNull();
                assertThat(session.duration()).isGreaterThanOrEqualTo(30);
                assertThat(session.duration()).isLessThanOrEqualTo(240);
                assertThat(session.intensity()).isNotNull();
                assertThat(session.tss()).isNotNull();
                assertThat(session.elevation()).isNotNull();
                assertThat(session.targetZone()).isNotNull();
            });
        }

        then(trainingPlanRepository).should().findAll();
    }

    @Test
    void shouldGenerateRandomTestDataWithVariedSessionTypes() {
        // given
        Long userId = 1L;
        Long planId = 1L;
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now().plusDays(30);

        TrainingPlan activePlan = TrainingPlan.builder()
                .id(planId)
                .userId(userId)
                .startDate(fromDate)
                .endDate(toDate.plusWeeks(12))
                .goal("General Fitness")
                .status("ACTIVE")
                .generatedAt(LocalDateTime.now())
                .build();

        given(trainingPlanRepository.findAll()).willReturn(List.of(activePlan));
        given(plannedSessionRepository.save(any(PlannedSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TrainingPlanDetailResponse response = trainingPlanService.generateRandomTestData(userId, fromDate, toDate);

        // then
        assertThat(response).isNotNull();

        // Collect all session types
        List<String> sessionTypes = new ArrayList<>();
        response.completedSessions().forEach(s -> sessionTypes.add(s.type()));
        response.trainingPlan().forEach(s -> sessionTypes.add(s.type()));

        // With a large enough date range, we should get some variety
        // (This test may occasionally fail due to randomness, but it's unlikely with 60 days)
        if (sessionTypes.size() > 5) {
            long distinctTypes = sessionTypes.stream().distinct().count();
            assertThat(distinctTypes).isGreaterThan(1);
        }

        then(trainingPlanRepository).should().findAll();
    }
}
