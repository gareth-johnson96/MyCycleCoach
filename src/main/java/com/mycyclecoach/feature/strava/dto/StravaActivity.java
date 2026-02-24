package com.mycyclecoach.feature.strava.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StravaActivity(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("distance") BigDecimal distance,
        @JsonProperty("moving_time") Integer movingTime,
        @JsonProperty("elapsed_time") Integer elapsedTime,
        @JsonProperty("total_elevation_gain") BigDecimal totalElevationGain,
        @JsonProperty("start_date") LocalDateTime startDate,
        @JsonProperty("average_speed") BigDecimal averageSpeed,
        @JsonProperty("max_speed") BigDecimal maxSpeed,
        @JsonProperty("average_watts") BigDecimal averageWatts,
        @JsonProperty("average_heartrate") BigDecimal averageHeartrate,
        @JsonProperty("max_heartrate") BigDecimal maxHeartrate,
        @JsonProperty("sport_type") String sportType,
        @JsonProperty("workout_type") Integer workoutType,
        @JsonProperty("type") String activityType) {}
