package com.mycyclecoach.feature.strava.scheduler;

import com.mycyclecoach.feature.strava.service.StravaSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "mycyclecoach.strava.sync", name = "enabled", havingValue = "true")
public class StravaScheduledSync {

    private final StravaSyncService stravaSyncService;

    @Scheduled(cron = "${mycyclecoach.strava.sync.cron}")
    public void scheduledRideSync() {
        log.info("Starting scheduled Strava ride sync");
        try {
            stravaSyncService.syncRidesForAllUsers();
            log.info("Completed scheduled Strava ride sync");
        } catch (Exception e) {
            log.error("Error during scheduled Strava ride sync", e);
        }
    }
}
