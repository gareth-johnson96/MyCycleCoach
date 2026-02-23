package com.mycyclecoach.feature.gpxanalysis.service;

import com.mycyclecoach.feature.gpxanalysis.domain.*;
import com.mycyclecoach.feature.gpxanalysis.repository.ClimbRepository;
import com.mycyclecoach.feature.gpxanalysis.repository.GpxFileRepository;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class GpxAnalysisServiceImpl implements GpxAnalysisService {

    private final GpxFileRepository gpxFileRepository;
    private final ClimbRepository climbRepository;
    private final GpxAnalysisMapper gpxAnalysisMapper;

    private static final double MIN_CLIMB_ELEVATION_METERS = 10.0;
    private static final double MIN_CLIMB_DISTANCE_METERS = 100.0;
    private static final double CLIMB_GRADIENT_THRESHOLD = 0.02;

    @Override
    @Transactional
    public GpxAnalysisResponse analyzeGpxFile(MultipartFile file, Long userId) {
        log.info("Analyzing GPX file filename={} for userId={}", file.getOriginalFilename(), userId);

        String content;
        try {
            content = new String(file.getBytes());
        } catch (IOException e) {
            throw new GpxParsingException("Failed to read GPX file content", e);
        }

        GpxFile gpxFile = GpxFile.builder()
                .filename(file.getOriginalFilename())
                .content(content)
                .userId(userId)
                .build();

        gpxFile = gpxFileRepository.save(gpxFile);
        log.info("Saved GPX file with id={}", gpxFile.getId());

        List<WayPoint> wayPoints = extractWayPoints(content);
        List<Climb> climbs = detectClimbs(wayPoints, gpxFile);
        climbs = climbRepository.saveAll(climbs);

        log.info("Detected and saved {} climbs for gpxFileId={}", climbs.size(), gpxFile.getId());

        return gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, climbs);
    }

    @Override
    @Transactional(readOnly = true)
    public GpxAnalysisResponse getGpxAnalysis(Long gpxFileId) {
        log.info("Retrieving GPX analysis for gpxFileId={}", gpxFileId);

        GpxFile gpxFile =
                gpxFileRepository.findById(gpxFileId).orElseThrow(() -> new GpxFileNotFoundException(gpxFileId));

        List<Climb> climbs = climbRepository.findByGpxFileId(gpxFileId);

        return gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, climbs);
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

                if (wayPoints.isEmpty()) {
                    throw new GpxParsingException("No waypoints found in GPX file");
                }

                return wayPoints;
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new GpxParsingException("Failed to parse GPX file", e);
        }
    }

    private List<Climb> detectClimbs(List<WayPoint> wayPoints, GpxFile gpxFile) {
        List<Climb> climbs = new ArrayList<>();

        if (wayPoints.size() < 2) {
            return climbs;
        }

        int climbStartIndex = -1;
        double climbStartElevation = 0;
        double climbDistance = 0;

        for (int i = 1; i < wayPoints.size(); i++) {
            WayPoint previous = wayPoints.get(i - 1);
            WayPoint current = wayPoints.get(i);

            if (previous.getElevation().isEmpty() || current.getElevation().isEmpty()) {
                continue;
            }

            double prevElevation = previous.getElevation().get().doubleValue();
            double currElevation = current.getElevation().get().doubleValue();
            double elevationDiff = currElevation - prevElevation;

            double segmentDistance = calculateDistance(previous, current);

            double gradient = segmentDistance > 0 ? elevationDiff / segmentDistance : 0;

            if (gradient >= CLIMB_GRADIENT_THRESHOLD) {
                if (climbStartIndex == -1) {
                    climbStartIndex = i - 1;
                    climbStartElevation = prevElevation;
                    climbDistance = 0;
                }
                climbDistance += segmentDistance;
            } else {
                if (climbStartIndex != -1) {
                    double elevationGain = prevElevation - climbStartElevation;
                    if (elevationGain >= MIN_CLIMB_ELEVATION_METERS && climbDistance >= MIN_CLIMB_DISTANCE_METERS) {
                        double avgGradient = climbDistance > 0 ? elevationGain / climbDistance : 0;

                        Climb climb = Climb.builder()
                                .gpxFile(gpxFile)
                                .distanceMeters(climbDistance)
                                .elevationGainMeters(elevationGain)
                                .averageGradient(avgGradient)
                                .startPointIndex(climbStartIndex)
                                .endPointIndex(i - 1)
                                .build();

                        climbs.add(climb);
                    }

                    climbStartIndex = -1;
                    climbDistance = 0;
                }
            }
        }

        if (climbStartIndex != -1) {
            WayPoint lastPoint = wayPoints.get(wayPoints.size() - 1);
            if (lastPoint.getElevation().isPresent()) {
                double elevationGain = lastPoint.getElevation().get().doubleValue() - climbStartElevation;
                if (elevationGain >= MIN_CLIMB_ELEVATION_METERS && climbDistance >= MIN_CLIMB_DISTANCE_METERS) {
                    double avgGradient = climbDistance > 0 ? elevationGain / climbDistance : 0;

                    Climb climb = Climb.builder()
                            .gpxFile(gpxFile)
                            .distanceMeters(climbDistance)
                            .elevationGainMeters(elevationGain)
                            .averageGradient(avgGradient)
                            .startPointIndex(climbStartIndex)
                            .endPointIndex(wayPoints.size() - 1)
                            .build();

                    climbs.add(climb);
                }
            }
        }

        return climbs;
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
