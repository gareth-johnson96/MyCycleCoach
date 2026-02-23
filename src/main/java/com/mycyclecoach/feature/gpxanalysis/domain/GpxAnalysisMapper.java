package com.mycyclecoach.feature.gpxanalysis.domain;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GpxAnalysisMapper {

    public ClimbResponse toClimbResponse(Climb climb) {
        return new ClimbResponse(
                climb.getId(),
                climb.getDistanceMeters(),
                climb.getElevationGainMeters(),
                climb.getAverageGradient(),
                climb.getStartPointIndex(),
                climb.getEndPointIndex());
    }

    public GpxAnalysisResponse toGpxAnalysisResponse(GpxFile gpxFile, List<Climb> climbs) {
        List<ClimbResponse> climbResponses =
                climbs.stream().map(this::toClimbResponse).toList();

        return new GpxAnalysisResponse(
                gpxFile.getId(), gpxFile.getFilename(), climbs.size(), climbResponses, gpxFile.getCreatedAt());
    }
}
