package com.mycyclecoach.feature.userprofile.dto;

import java.math.BigDecimal;

public record ProfileResponse(
        Long id,
        Long userId,
        Integer age,
        BigDecimal weight,
        BigDecimal height,
        String experienceLevel,
        Integer currentFtp,
        Integer maxHr) {}
