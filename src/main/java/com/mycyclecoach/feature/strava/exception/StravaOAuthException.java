package com.mycyclecoach.feature.strava.exception;

public class StravaOAuthException extends RuntimeException {
    public StravaOAuthException(String message) {
        super(message);
    }

    public StravaOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
