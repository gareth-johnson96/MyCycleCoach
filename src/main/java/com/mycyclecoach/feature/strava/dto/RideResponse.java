package com.mycyclecoach.feature.strava.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideResponse(
        Long id,
        Long stravaActivityId,
        String name,
        BigDecimal distance,
        Integer movingTime,
        Integer elapsedTime,
        BigDecimal totalElevationGain,
        LocalDateTime startDate,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed,
        BigDecimal averageWatts,
        BigDecimal averageHeartrate,
        BigDecimal maxHeartrate) {}
