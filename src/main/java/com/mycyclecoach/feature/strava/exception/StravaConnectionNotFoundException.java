package com.mycyclecoach.feature.strava.exception;

public class StravaConnectionNotFoundException extends RuntimeException {
    public StravaConnectionNotFoundException(Long userId) {
        super("Strava connection not found for user: " + userId);
    }
}
