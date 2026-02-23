package com.mycyclecoach.feature.userprofile.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "training_backgrounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingBackground {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "years_training")
    private Integer yearsTraining;

    @Column(name = "weekly_volume")
    private Integer weeklyVolume;

    @Column(name = "recent_injuries")
    private String recentInjuries;

    @Column(name = "prior_events")
    private String priorEvents;

    @Column(name = "training_history")
    private String trainingHistory;

    @Column(name = "injury_history")
    private String injuryHistory;

    @Column(name = "daily_availability")
    private String dailyAvailability;

    @Column(name = "weekly_training_times")
    private String weeklyTrainingTimes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
