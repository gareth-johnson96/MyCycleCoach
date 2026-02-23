package com.mycyclecoach.feature.gpxanalysis.domain;

public record ClimbResponse(
        Long id,
        Double distanceMeters,
        Double elevationGainMeters,
        Double averageGradient,
        Integer startPointIndex,
        Integer endPointIndex) {}
