package com.mycyclecoach.feature.trainingplan.repository;

import com.mycyclecoach.feature.trainingplan.domain.PlannedSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlannedSessionRepository extends JpaRepository<PlannedSession, Long> {

    List<PlannedSession> findByPlanId(Long planId);
}
