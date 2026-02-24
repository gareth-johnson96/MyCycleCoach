package com.mycyclecoach.feature.strava.service;

import com.mycyclecoach.feature.gpxanalysis.domain.GpxFile;
import com.mycyclecoach.feature.gpxanalysis.repository.GpxFileRepository;
import com.mycyclecoach.feature.strava.client.StravaApiClient;
import com.mycyclecoach.feature.strava.domain.Ride;
import com.mycyclecoach.feature.strava.domain.StravaConnection;
import com.mycyclecoach.feature.strava.dto.RideResponse;
import com.mycyclecoach.feature.strava.dto.StravaActivity;
import com.mycyclecoach.feature.strava.exception.StravaConnectionNotFoundException;
import com.mycyclecoach.feature.strava.repository.RideRepository;
import com.mycyclecoach.feature.strava.repository.StravaConnectionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StravaSyncServiceImpl implements StravaSyncService {

    private final StravaApiClient stravaApiClient;
    private final StravaConnectionRepository stravaConnectionRepository;
    private final RideRepository rideRepository;
    private final StravaAuthService stravaAuthService;
    private final GpxFileRepository gpxFileRepository;

    @Override
    @Transactional
    public void syncRidesForUser(Long userId) {
        log.info("Starting ride sync for user: {}", userId);

        StravaConnection connection = stravaConnectionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new StravaConnectionNotFoundException(userId));

        stravaAuthService.refreshTokenIfNeeded(userId);

        connection = stravaConnectionRepository.findByUserId(userId).orElseThrow();

        try {
            int page = 1;
            int perPage = 30;
            boolean hasMore = true;

            while (hasMore) {
                List<StravaActivity> activities =
                        stravaApiClient.getAthleteActivities(connection.getAccessToken(), perPage, page);

                if (activities == null || activities.isEmpty()) {
                    hasMore = false;
                    break;
                }

                for (StravaActivity activity : activities) {
                    if (!rideRepository.existsByStravaActivityId(activity.id())) {
                        // Try to download GPX data for the activity
                        Long gpxFileId = null;
                        try {
                            String gpxContent =
                                    stravaApiClient.getActivityGpx(connection.getAccessToken(), activity.id());
                            if (gpxContent != null && !gpxContent.isEmpty()) {
                                // Save GPX file
                                GpxFile gpxFile = GpxFile.builder()
                                        .filename(activity.id() + "_" + sanitizeFilename(activity.name()) + ".gpx")
                                        .content(gpxContent)
                                        .userId(userId)
                                        .build();
                                gpxFile = gpxFileRepository.save(gpxFile);
                                gpxFileId = gpxFile.getId();
                                log.info("Saved GPX file for activity: {} with id: {}", activity.id(), gpxFileId);
                            } else {
                                log.debug("No GPX data available for activity: {}", activity.id());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to download GPX for activity {}: {}", activity.id(), e.getMessage());
                        }

                        // Save ride with all fields including GPX reference
                        Ride ride = Ride.builder()
                                .userId(userId)
                                .stravaActivityId(activity.id())
                                .name(activity.name())
                                .distance(activity.distance())
                                .movingTime(activity.movingTime())
                                .elapsedTime(activity.elapsedTime())
                                .totalElevationGain(activity.totalElevationGain())
                                .startDate(activity.startDate())
                                .averageSpeed(activity.averageSpeed())
                                .maxSpeed(activity.maxSpeed())
                                .averageWatts(activity.averageWatts())
                                .averageHeartrate(activity.averageHeartrate())
                                .maxHeartrate(activity.maxHeartrate())
                                .sportType(activity.sportType())
                                .workoutType(activity.workoutType())
                                .activityType(activity.activityType())
                                .gpxFileId(gpxFileId)
                                .build();

                        rideRepository.save(ride);
                        log.info("Saved new ride: {} for user: {}", activity.name(), userId);
                    }
                }

                if (activities.size() < perPage) {
                    hasMore = false;
                } else {
                    page++;
                }
            }

            log.info("Completed ride sync for user: {}", userId);
        } catch (Exception e) {
            log.error("Error syncing rides for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void syncRidesForAllUsers() {
        log.info("Starting ride sync for all connected users");

        List<StravaConnection> connections = stravaConnectionRepository.findAll();

        log.info("Found {} connected users to sync", connections.size());

        for (StravaConnection connection : connections) {
            try {
                syncRidesForUser(connection.getUserId());
            } catch (Exception e) {
                log.error("Failed to sync rides for user: {}", connection.getUserId(), e);
            }
        }

        log.info("Completed ride sync for all users");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> getUserRides(Long userId) {
        log.info("Fetching rides for user: {}", userId);

        List<Ride> rides = rideRepository.findByUserId(userId);

        return rides.stream()
                .map(ride -> new RideResponse(
                        ride.getId(),
                        ride.getStravaActivityId(),
                        ride.getName(),
                        ride.getDistance(),
                        ride.getMovingTime(),
                        ride.getElapsedTime(),
                        ride.getTotalElevationGain(),
                        ride.getStartDate(),
                        ride.getAverageSpeed(),
                        ride.getMaxSpeed(),
                        ride.getAverageWatts(),
                        ride.getAverageHeartrate(),
                        ride.getMaxHeartrate(),
                        ride.getSportType(),
                        ride.getWorkoutType(),
                        ride.getActivityType(),
                        ride.getGpxFileId()))
                .collect(Collectors.toList());
    }

    private String sanitizeFilename(String name) {
        if (name == null) {
            return "activity";
        }
        // Replace any characters that aren't alphanumeric, dash, underscore, or space
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_ ]", "_").replaceAll("\\s+", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 50));
    }
}
