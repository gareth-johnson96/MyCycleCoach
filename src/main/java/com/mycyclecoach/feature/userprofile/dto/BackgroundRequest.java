package com.mycyclecoach.feature.userprofile.dto;

public record BackgroundRequest(
        Integer yearsTraining, Integer weeklyVolume, String recentInjuries, String priorEvents) {}
