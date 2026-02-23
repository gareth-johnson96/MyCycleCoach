package com.mycyclecoach.feature.userprofile.dto;

public record BackgroundResponse(
        Long id,
        Long userId,
        Integer yearsTraining,
        Integer weeklyVolume,
        String trainingHistory,
        String injuryHistory,
        String recentInjuries,
        String priorEvents,
        String dailyAvailability,
        String weeklyTrainingTimes) {}
