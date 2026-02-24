package com.mycyclecoach.feature.strava.repository;

import com.mycyclecoach.feature.strava.domain.Ride;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByUserId(Long userId);

    Optional<Ride> findByStravaActivityId(Long stravaActivityId);

    boolean existsByStravaActivityId(Long stravaActivityId);

    List<Ride> findByUserIdAndStartDateAfter(Long userId, LocalDateTime startDate);
}
