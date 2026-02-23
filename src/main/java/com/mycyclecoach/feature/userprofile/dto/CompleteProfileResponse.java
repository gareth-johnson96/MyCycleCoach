package com.mycyclecoach.feature.userprofile.dto;

public record CompleteProfileResponse(
        ProfileResponse profile, BackgroundResponse background, GoalsResponse goals) {}
