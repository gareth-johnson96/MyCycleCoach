package com.mycyclecoach.feature.strava.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_at") Long expiresAt,
        @JsonProperty("athlete") StravaAthlete athlete) {

    public record StravaAthlete(@JsonProperty("id") Long id) {}
}
