package com.mycyclecoach.feature.gpxanalysis.domain;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GpxAnalysisMapper {

    private static final double FLAT_SPEED_KMH = 25.0;
    private static final double CLIMB_BASE_SPEED_KMH = 15.0;
    private static final double STEEP_CLIMB_SPEED_KMH = 8.0;
    private static final double STEEP_GRADIENT_THRESHOLD = 0.06;

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

        List<WayPoint> wayPoints = extractWayPoints(gpxFile.getContent());
        double totalDistanceKm = calculateTotalDistance(wayPoints);
        double estimatedTimeMinutes = calculateEstimatedTime(wayPoints, climbs);

        return new GpxAnalysisResponse(
                gpxFile.getId(),
                gpxFile.getFilename(),
                climbs.size(),
                climbResponses,
                totalDistanceKm,
                estimatedTimeMinutes,
                gpxFile.getCreatedAt());
    }

    private List<WayPoint> extractWayPoints(String gpxContent) {
        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("gpx", ".gpx");
            try {
                java.nio.file.Files.writeString(tempFile, gpxContent);
                GPX gpx = GPX.read(tempFile);

                List<WayPoint> wayPoints = new ArrayList<>();
                gpx.tracks()
                        .flatMap(track -> track.segments())
                        .flatMap(segment -> segment.points())
                        .forEach(wayPoints::add);

                return wayPoints;
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.error("Failed to extract waypoints from GPX content", e);
            return new ArrayList<>();
        }
    }

    private double calculateTotalDistance(List<WayPoint> wayPoints) {
        if (wayPoints.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 1; i < wayPoints.size(); i++) {
            totalDistance += calculateDistance(wayPoints.get(i - 1), wayPoints.get(i));
        }

        return totalDistance / 1000.0; // Convert to kilometers
    }

    private double calculateEstimatedTime(List<WayPoint> wayPoints, List<Climb> climbs) {
        if (wayPoints.size() < 2) {
            return 0.0;
        }

        double totalTimeMinutes = 0.0;

        for (int i = 1; i < wayPoints.size(); i++) {
            WayPoint previous = wayPoints.get(i - 1);
            WayPoint current = wayPoints.get(i);

            double segmentDistance = calculateDistance(previous, current);
            double segmentTimeMinutes = 0.0;

            boolean isInClimb = isPointInAnyClimb(i - 1, i, climbs);

            if (isInClimb) {
                Climb relevantClimb = findClimbContainingSegment(i - 1, i, climbs);
                if (relevantClimb != null) {
                    double gradient = relevantClimb.getAverageGradient();
                    double speedKmh =
                            gradient >= STEEP_GRADIENT_THRESHOLD ? STEEP_CLIMB_SPEED_KMH : CLIMB_BASE_SPEED_KMH;
                    segmentTimeMinutes = (segmentDistance / 1000.0) / speedKmh * 60.0;
                } else {
                    segmentTimeMinutes = (segmentDistance / 1000.0) / FLAT_SPEED_KMH * 60.0;
                }
            } else {
                segmentTimeMinutes = (segmentDistance / 1000.0) / FLAT_SPEED_KMH * 60.0;
            }

            totalTimeMinutes += segmentTimeMinutes;
        }

        return Math.round(totalTimeMinutes * 10.0) / 10.0; // Round to 1 decimal place
    }

    private boolean isPointInAnyClimb(int startIdx, int endIdx, List<Climb> climbs) {
        return climbs.stream().anyMatch(climb -> isPointInClimb(startIdx, endIdx, climb));
    }

    private boolean isPointInClimb(int startIdx, int endIdx, Climb climb) {
        return (startIdx >= climb.getStartPointIndex() && startIdx <= climb.getEndPointIndex())
                || (endIdx >= climb.getStartPointIndex() && endIdx <= climb.getEndPointIndex());
    }

    private Climb findClimbContainingSegment(int startIdx, int endIdx, List<Climb> climbs) {
        return climbs.stream()
                .filter(climb -> isPointInClimb(startIdx, endIdx, climb))
                .findFirst()
                .orElse(null);
    }

    private double calculateDistance(WayPoint p1, WayPoint p2) {
        double lat1 = Math.toRadians(p1.getLatitude().doubleValue());
        double lon1 = Math.toRadians(p1.getLongitude().doubleValue());
        double lat2 = Math.toRadians(p2.getLatitude().doubleValue());
        double lon2 = Math.toRadians(p2.getLongitude().doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        final double EARTH_RADIUS_METERS = 6371000;
        return EARTH_RADIUS_METERS * c;
    }
}
