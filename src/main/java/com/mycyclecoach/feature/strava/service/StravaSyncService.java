package com.mycyclecoach.feature.strava.service;

import com.mycyclecoach.feature.strava.dto.RideResponse;
import java.util.List;

public interface StravaSyncService {

    void syncRidesForUser(Long userId);

    void syncRidesForAllUsers();

    List<RideResponse> getUserRides(Long userId);
}
