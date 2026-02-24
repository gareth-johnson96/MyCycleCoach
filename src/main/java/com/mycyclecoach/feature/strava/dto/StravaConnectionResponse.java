package com.mycyclecoach.feature.strava.dto;

import java.time.LocalDateTime;

public record StravaConnectionResponse(
        Long id, Long userId, Long stravaAthleteId, boolean connected, LocalDateTime connectedAt) {}
