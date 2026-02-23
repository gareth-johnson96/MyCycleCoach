package com.mycyclecoach.feature.userprofile.service;

import com.mycyclecoach.feature.userprofile.dto.BackgroundRequest;
import com.mycyclecoach.feature.userprofile.dto.GoalsRequest;
import com.mycyclecoach.feature.userprofile.dto.ProfileResponse;
import com.mycyclecoach.feature.userprofile.dto.UpdateProfileRequest;

public interface UserProfileService {

    ProfileResponse getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void saveBackground(Long userId, BackgroundRequest request);

    void updateGoals(Long userId, GoalsRequest request);
}
