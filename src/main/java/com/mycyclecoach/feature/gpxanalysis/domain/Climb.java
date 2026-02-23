package com.mycyclecoach.feature.gpxanalysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "climbs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Climb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpx_file_id", nullable = false)
    private GpxFile gpxFile;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false)
    private Double elevationGainMeters;

    @Column(nullable = false)
    private Double averageGradient;

    @Column(nullable = false)
    private Integer startPointIndex;

    @Column(nullable = false)
    private Integer endPointIndex;
}
