package com.mycyclecoach.feature.userprofile.service;

import com.mycyclecoach.feature.userprofile.domain.TrainingBackground;
import com.mycyclecoach.feature.userprofile.domain.TrainingGoals;
import com.mycyclecoach.feature.userprofile.domain.UserProfile;
import com.mycyclecoach.feature.userprofile.dto.BackgroundRequest;
import com.mycyclecoach.feature.userprofile.dto.GoalsRequest;
import com.mycyclecoach.feature.userprofile.dto.ProfileResponse;
import com.mycyclecoach.feature.userprofile.dto.UpdateProfileRequest;
import com.mycyclecoach.feature.userprofile.repository.TrainingBackgroundRepository;
import com.mycyclecoach.feature.userprofile.repository.TrainingGoalsRepository;
import com.mycyclecoach.feature.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final TrainingBackgroundRepository trainingBackgroundRepository;
    private final TrainingGoalsRepository trainingGoalsRepository;

    @Override
    public ProfileResponse getProfile(Long userId) {
        UserProfile profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for userId: " + userId));

        return new ProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getAge(),
                profile.getWeight(),
                profile.getExperienceLevel());
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for userId: " + userId));

        profile.setAge(request.age());
        profile.setWeight(request.weight());
        profile.setExperienceLevel(request.experienceLevel());

        userProfileRepository.save(profile);
        log.info("User profile updated for userId: {}", userId);
    }

    @Override
    @Transactional
    public void saveBackground(Long userId, BackgroundRequest request) {
        TrainingBackground background = trainingBackgroundRepository
                .findByUserId(userId)
                .orElse(TrainingBackground.builder().userId(userId).build());

        background.setYearsTraining(request.yearsTraining());
        background.setWeeklyVolume(request.weeklyVolume());
        background.setRecentInjuries(request.recentInjuries());
        background.setPriorEvents(request.priorEvents());

        trainingBackgroundRepository.save(background);
        log.info("Training background saved for userId: {}", userId);
    }

    @Override
    @Transactional
    public void updateGoals(Long userId, GoalsRequest request) {
        TrainingGoals goals = trainingGoalsRepository
                .findByUserId(userId)
                .orElse(TrainingGoals.builder().userId(userId).build());

        goals.setGoals(request.goals());

        trainingGoalsRepository.save(goals);
        log.info("Training goals updated for userId: {}", userId);
    }
}
