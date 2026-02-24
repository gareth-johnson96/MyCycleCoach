package com.mycyclecoach.feature.gpxanalysis.domain;

import java.time.LocalDateTime;

public record GpxFileResponse(
        Long id, String filename, Long userId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
