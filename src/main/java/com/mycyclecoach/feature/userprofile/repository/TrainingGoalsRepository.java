package com.mycyclecoach.feature.userprofile.repository;

import com.mycyclecoach.feature.userprofile.domain.TrainingGoals;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingGoalsRepository extends JpaRepository<TrainingGoals, Long> {

    Optional<TrainingGoals> findByUserId(Long userId);
}
