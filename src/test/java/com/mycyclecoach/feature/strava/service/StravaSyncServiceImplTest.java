package com.mycyclecoach.feature.strava.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

import com.mycyclecoach.feature.strava.client.StravaApiClient;
import com.mycyclecoach.feature.strava.domain.Ride;
import com.mycyclecoach.feature.strava.domain.StravaConnection;
import com.mycyclecoach.feature.strava.dto.RideResponse;
import com.mycyclecoach.feature.strava.dto.StravaActivity;
import com.mycyclecoach.feature.strava.exception.StravaConnectionNotFoundException;
import com.mycyclecoach.feature.strava.repository.RideRepository;
import com.mycyclecoach.feature.strava.repository.StravaConnectionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StravaSyncServiceImplTest {

    @Mock
    private StravaApiClient stravaApiClient;

    @Mock
    private StravaConnectionRepository stravaConnectionRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private StravaAuthService stravaAuthService;

    @Mock
    private com.mycyclecoach.feature.gpxanalysis.repository.GpxFileRepository gpxFileRepository;

    @InjectMocks
    private StravaSyncServiceImpl stravaSyncService;

    @Test
    void shouldThrowExceptionWhenUserHasNoConnection() {
        // given
        Long userId = 1L;
        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> stravaSyncService.syncRidesForUser(userId))
                .isInstanceOf(StravaConnectionNotFoundException.class);
    }

    @Test
    void shouldSyncRidesForUserWhenConnectionExists() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .userId(userId)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        StravaActivity activity = new StravaActivity(
                12345L,
                "Morning Ride",
                new BigDecimal("25000"),
                3600,
                3700,
                new BigDecimal("250"),
                LocalDateTime.now(),
                new BigDecimal("6.94"),
                new BigDecimal("12.5"),
                null,
                null,
                null,
                "Ride",
                null,
                "Ride");

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(userId);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(1)))
                .willReturn(List.of(activity));
        given(rideRepository.existsByStravaActivityId(12345L)).willReturn(false);
        given(rideRepository.save(any(Ride.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaSyncService.syncRidesForUser(userId);

        // then
        then(stravaAuthService).should().refreshTokenIfNeeded(userId);
        then(rideRepository).should().save(any(Ride.class));
    }

    @Test
    void shouldNotSaveDuplicateRides() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .userId(userId)
                .accessToken("access-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        StravaActivity activity = new StravaActivity(
                12345L,
                "Morning Ride",
                new BigDecimal("25000"),
                3600,
                3700,
                new BigDecimal("250"),
                LocalDateTime.now(),
                new BigDecimal("6.94"),
                new BigDecimal("12.5"),
                null,
                null,
                null,
                "Ride",
                null,
                "Ride");

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(userId);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(1)))
                .willReturn(List.of(activity));
        given(rideRepository.existsByStravaActivityId(12345L)).willReturn(true);

        // when
        stravaSyncService.syncRidesForUser(userId);

        // then
        then(rideRepository).should(never()).save(any(Ride.class));
    }

    @Test
    void shouldReturnUserRides() {
        // given
        Long userId = 1L;
        Ride ride = Ride.builder()
                .id(1L)
                .userId(userId)
                .stravaActivityId(12345L)
                .name("Morning Ride")
                .distance(new BigDecimal("25000"))
                .movingTime(3600)
                .elapsedTime(3700)
                .totalElevationGain(new BigDecimal("250"))
                .startDate(LocalDateTime.now())
                .build();

        given(rideRepository.findByUserId(userId)).willReturn(List.of(ride));

        // when
        List<RideResponse> rides = stravaSyncService.getUserRides(userId);

        // then
        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).name()).isEqualTo("Morning Ride");
        assertThat(rides.get(0).stravaActivityId()).isEqualTo(12345L);
    }

    @Test
    void shouldSyncRidesForAllConnectedUsers() {
        // given
        StravaConnection connection1 = StravaConnection.builder()
                .userId(1L)
                .accessToken("token1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        StravaConnection connection2 = StravaConnection.builder()
                .userId(2L)
                .accessToken("token2")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        given(stravaConnectionRepository.findAll()).willReturn(List.of(connection1, connection2));
        given(stravaConnectionRepository.findByUserId(1L)).willReturn(Optional.of(connection1));
        given(stravaConnectionRepository.findByUserId(2L)).willReturn(Optional.of(connection2));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(any());
        given(stravaApiClient.getAthleteActivities(any(), eq(30), eq(1))).willReturn(List.of());

        // when
        stravaSyncService.syncRidesForAllUsers();

        // then
        then(stravaAuthService).should(times(2)).refreshTokenIfNeeded(any());
    }

    @Test
    void shouldHandleMultiplePagesOfActivities() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .userId(userId)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Create 30 activities for page 1 (full page)
        List<StravaActivity> page1 = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            page1.add(new StravaActivity(
                    (long) i,
                    "Ride " + i,
                    new BigDecimal("25000"),
                    3600,
                    3700,
                    new BigDecimal("250"),
                    LocalDateTime.now(),
                    new BigDecimal("6.94"),
                    new BigDecimal("12.5"),
                    null,
                    null,
                    null,
                    "Ride",
                    null,
                    "Ride"));
        }

        // Create 15 activities for page 2 (partial page)
        List<StravaActivity> page2 = new java.util.ArrayList<>();
        for (int i = 31; i <= 45; i++) {
            page2.add(new StravaActivity(
                    (long) i,
                    "Ride " + i,
                    new BigDecimal("25000"),
                    3600,
                    3700,
                    new BigDecimal("250"),
                    LocalDateTime.now(),
                    new BigDecimal("6.94"),
                    new BigDecimal("12.5"),
                    null,
                    null,
                    null,
                    "Ride",
                    null,
                    "Ride"));
        }

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(userId);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(1)))
                .willReturn(page1);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(2)))
                .willReturn(page2);
        given(rideRepository.existsByStravaActivityId(anyLong())).willReturn(false);
        given(rideRepository.save(any(Ride.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaSyncService.syncRidesForUser(userId);

        // then
        then(rideRepository).should(times(45)).save(any(Ride.class));
    }

    @Test
    void shouldDownloadAndStoreGpxWhenAvailable() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .userId(userId)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        StravaActivity activity = new StravaActivity(
                12345L,
                "Morning Ride with Special! Chars & @#$",
                new BigDecimal("25000"),
                3600,
                3700,
                new BigDecimal("250"),
                LocalDateTime.now(),
                new BigDecimal("6.94"),
                new BigDecimal("12.5"),
                null,
                null,
                null,
                "Ride",
                null,
                "Ride");

        String gpxContent = "<?xml version=\"1.0\"?><gpx>test content</gpx>";
        com.mycyclecoach.feature.gpxanalysis.domain.GpxFile savedGpxFile =
                com.mycyclecoach.feature.gpxanalysis.domain.GpxFile.builder()
                        .id(100L)
                        .filename("12345_Morning_Ride_with_Special__Chars______.gpx")
                        .content(gpxContent)
                        .userId(userId)
                        .build();

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(userId);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(1)))
                .willReturn(List.of(activity));
        given(stravaApiClient.getActivityGpx("access-token", 12345L)).willReturn(gpxContent);
        given(gpxFileRepository.save(any(com.mycyclecoach.feature.gpxanalysis.domain.GpxFile.class)))
                .willReturn(savedGpxFile);
        given(rideRepository.existsByStravaActivityId(12345L)).willReturn(false);
        given(rideRepository.save(any(Ride.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaSyncService.syncRidesForUser(userId);

        // then
        then(stravaApiClient).should().getActivityGpx("access-token", 12345L);
        then(gpxFileRepository).should().save(any(com.mycyclecoach.feature.gpxanalysis.domain.GpxFile.class));
        then(rideRepository).should().save(argThat(ride -> ride.getGpxFileId() != null && ride.getGpxFileId() == 100L));
    }

    @Test
    void shouldHandleGpxDownloadFailureGracefully() {
        // given
        Long userId = 1L;
        StravaConnection connection = StravaConnection.builder()
                .userId(userId)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        StravaActivity activity = new StravaActivity(
                12345L,
                "Morning Ride",
                new BigDecimal("25000"),
                3600,
                3700,
                new BigDecimal("250"),
                LocalDateTime.now(),
                new BigDecimal("6.94"),
                new BigDecimal("12.5"),
                null,
                null,
                null,
                "Ride",
                null,
                "Ride");

        given(stravaConnectionRepository.findByUserId(userId)).willReturn(Optional.of(connection));
        willDoNothing().given(stravaAuthService).refreshTokenIfNeeded(userId);
        given(stravaApiClient.getAthleteActivities(eq("access-token"), eq(30), eq(1)))
                .willReturn(List.of(activity));
        given(stravaApiClient.getActivityGpx("access-token", 12345L)).willReturn(null);
        given(rideRepository.existsByStravaActivityId(12345L)).willReturn(false);
        given(rideRepository.save(any(Ride.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        stravaSyncService.syncRidesForUser(userId);

        // then
        then(rideRepository).should().save(argThat(ride -> ride.getGpxFileId() == null));
        then(gpxFileRepository).should(never()).save(any(com.mycyclecoach.feature.gpxanalysis.domain.GpxFile.class));
    }
}
