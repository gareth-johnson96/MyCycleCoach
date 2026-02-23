package com.mycyclecoach.feature.userprofile.service;

import com.mycyclecoach.feature.userprofile.dto.*;

public interface UserProfileService {

    ProfileResponse getProfile(Long userId);

    BackgroundResponse getBackground(Long userId);

    GoalsResponse getGoals(Long userId);

    CompleteProfileResponse getCompleteProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void saveBackground(Long userId, BackgroundRequest request);

    void updateGoals(Long userId, GoalsRequest request);

    void submitQuestionnaire(Long userId, QuestionnaireRequest request);
}
