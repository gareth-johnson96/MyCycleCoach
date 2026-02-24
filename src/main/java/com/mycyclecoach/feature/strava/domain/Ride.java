package com.mycyclecoach.feature.strava.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "strava_activity_id", nullable = false, unique = true)
    private Long stravaActivityId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "distance")
    private BigDecimal distance;

    @Column(name = "moving_time")
    private Integer movingTime;

    @Column(name = "elapsed_time")
    private Integer elapsedTime;

    @Column(name = "total_elevation_gain")
    private BigDecimal totalElevationGain;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "average_speed")
    private BigDecimal averageSpeed;

    @Column(name = "max_speed")
    private BigDecimal maxSpeed;

    @Column(name = "average_watts")
    private BigDecimal averageWatts;

    @Column(name = "average_heartrate")
    private BigDecimal averageHeartrate;

    @Column(name = "max_heartrate")
    private BigDecimal maxHeartrate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
