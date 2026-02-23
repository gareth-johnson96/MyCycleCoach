package com.mycyclecoach.feature.userprofile.dto;

import java.time.LocalDateTime;

public record GoalsResponse(
        Long id, Long userId, String goals, String targetEvent, LocalDateTime targetEventDate) {}
