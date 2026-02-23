package com.mycyclecoach.feature.userprofile.service;

import com.mycyclecoach.feature.userprofile.domain.TrainingBackground;
import com.mycyclecoach.feature.userprofile.domain.TrainingGoals;
import com.mycyclecoach.feature.userprofile.domain.UserProfile;
import com.mycyclecoach.feature.userprofile.dto.*;
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
                profile.getHeight(),
                profile.getExperienceLevel(),
                profile.getCurrentFtp(),
                profile.getMaxHr());
    }

    @Override
    public BackgroundResponse getBackground(Long userId) {
        TrainingBackground background = trainingBackgroundRepository
                .findByUserId(userId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Training background not found for userId: " + userId));

        return new BackgroundResponse(
                background.getId(),
                background.getUserId(),
                background.getYearsTraining(),
                background.getWeeklyVolume(),
                background.getTrainingHistory(),
                background.getInjuryHistory(),
                background.getRecentInjuries(),
                background.getPriorEvents(),
                background.getDailyAvailability(),
                background.getWeeklyTrainingTimes());
    }

    @Override
    public GoalsResponse getGoals(Long userId) {
        TrainingGoals goals = trainingGoalsRepository
                .findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Training goals not found for userId: " + userId));

        return new GoalsResponse(
                goals.getId(),
                goals.getUserId(),
                goals.getGoals(),
                goals.getTargetEvent(),
                goals.getTargetEventDate());
    }

    @Override
    public CompleteProfileResponse getCompleteProfile(Long userId) {
        ProfileResponse profile = getProfile(userId);
        BackgroundResponse background = getBackground(userId);
        GoalsResponse goals = getGoals(userId);
        return new CompleteProfileResponse(profile, background, goals);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for userId: " + userId));

        profile.setAge(request.age());
        profile.setWeight(request.weight());
        profile.setHeight(request.height());
        profile.setExperienceLevel(request.experienceLevel());
        profile.setCurrentFtp(request.currentFtp());
        profile.setMaxHr(request.maxHr());

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
        background.setTrainingHistory(request.trainingHistory());
        background.setInjuryHistory(request.injuryHistory());
        background.setRecentInjuries(request.recentInjuries());
        background.setPriorEvents(request.priorEvents());
        background.setDailyAvailability(request.dailyAvailability());
        background.setWeeklyTrainingTimes(request.weeklyTrainingTimes());

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
        goals.setTargetEvent(request.targetEvent());
        goals.setTargetEventDate(request.targetEventDate());

        trainingGoalsRepository.save(goals);
        log.info("Training goals updated for userId: {}", userId);
    }

    @Override
    @Transactional
    public void submitQuestionnaire(Long userId, QuestionnaireRequest request) {
        log.info("Processing questionnaire submission for userId: {}", userId);

        // Create or update user profile
        UserProfile profile = userProfileRepository
                .findByUserId(userId)
                .orElse(UserProfile.builder().userId(userId).build());

        profile.setAge(request.age());
        profile.setWeight(request.weight());
        profile.setHeight(request.height());
        profile.setExperienceLevel(request.experienceLevel());
        profile.setCurrentFtp(request.currentFtp());
        profile.setMaxHr(request.maxHr());
        userProfileRepository.save(profile);

        // Create or update training background
        TrainingBackground background = trainingBackgroundRepository
                .findByUserId(userId)
                .orElse(TrainingBackground.builder().userId(userId).build());

        background.setYearsTraining(request.yearsTraining());
        background.setWeeklyVolume(request.weeklyVolume());
        background.setTrainingHistory(request.trainingHistory());
        background.setInjuryHistory(request.injuryHistory());
        background.setRecentInjuries(request.recentInjuries());
        background.setPriorEvents(request.priorEvents());
        background.setDailyAvailability(request.dailyAvailability());
        background.setWeeklyTrainingTimes(request.weeklyTrainingTimes());
        trainingBackgroundRepository.save(background);

        // Create or update training goals
        TrainingGoals goals = trainingGoalsRepository
                .findByUserId(userId)
                .orElse(TrainingGoals.builder().userId(userId).build());

        goals.setGoals(request.goals());
        goals.setTargetEvent(request.targetEvent());
        goals.setTargetEventDate(request.targetEventDate());
        trainingGoalsRepository.save(goals);

        log.info("Questionnaire successfully saved for userId: {}", userId);
    }
}
