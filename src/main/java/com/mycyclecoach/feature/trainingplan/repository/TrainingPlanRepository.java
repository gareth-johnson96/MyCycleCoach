package com.mycyclecoach.feature.trainingplan.repository;

import com.mycyclecoach.feature.trainingplan.domain.TrainingPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    @Query("SELECT tp FROM TrainingPlan tp WHERE tp.userId = :userId AND tp.status = :status")
    Optional<TrainingPlan> findActiveByUserId(@Param("userId") Long userId, @Param("status") String status);
}
