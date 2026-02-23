package com.mycyclecoach.feature.trainingplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlannedSessionResponse(
        Long id,
        Long planId,
        LocalDate scheduledDate,
        String type,
        BigDecimal distance,
        Integer duration,
        String intensity,
        String status) {}
