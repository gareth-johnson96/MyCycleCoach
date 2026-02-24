package com.mycyclecoach.feature.strava.repository;

import com.mycyclecoach.feature.strava.domain.StravaConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StravaConnectionRepository extends JpaRepository<StravaConnection, Long> {

    Optional<StravaConnection> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<StravaConnection> findAll();
}
