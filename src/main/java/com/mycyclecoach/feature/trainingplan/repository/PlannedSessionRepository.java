package com.mycyclecoach.feature.trainingplan.repository;

import com.mycyclecoach.feature.trainingplan.domain.PlannedSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlannedSessionRepository extends JpaRepository<PlannedSession, Long> {

    List<PlannedSession> findByPlanId(Long planId);

    List<PlannedSession> findByPlanIdAndScheduledDateBetween(Long planId, LocalDate fromDate, LocalDate toDate);

    List<PlannedSession> findByPlanIdAndStatus(Long planId, String status);

    List<PlannedSession> findByPlanIdAndStatusAndScheduledDateBetween(
            Long planId, String status, LocalDate fromDate, LocalDate toDate);
}
