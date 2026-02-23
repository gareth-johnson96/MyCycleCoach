package com.mycyclecoach.feature.gpxanalysis.domain;

import java.time.LocalDateTime;
import java.util.List;

public record GpxAnalysisResponse(
        Long gpxFileId, String filename, Integer climbCount, List<ClimbResponse> climbs, LocalDateTime uploadedAt) {}
