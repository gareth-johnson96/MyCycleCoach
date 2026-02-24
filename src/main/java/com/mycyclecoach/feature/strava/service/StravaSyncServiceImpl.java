package com.mycyclecoach.feature.strava.service;

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
                        ride.getMaxHeartrate()))
                .collect(Collectors.toList());
    }
}
