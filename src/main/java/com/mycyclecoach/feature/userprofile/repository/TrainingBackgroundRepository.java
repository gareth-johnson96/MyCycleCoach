package com.mycyclecoach.feature.userprofile.repository;

import com.mycyclecoach.feature.userprofile.domain.TrainingBackground;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingBackgroundRepository extends JpaRepository<TrainingBackground, Long> {

    Optional<TrainingBackground> findByUserId(Long userId);
}
