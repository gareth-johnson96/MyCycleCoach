package com.mycyclecoach.feature.gpxanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GpxAnalysisMapperTest {

    @InjectMocks
    private GpxAnalysisMapper gpxAnalysisMapper;

    @Test
    void shouldMapClimbToClimbResponse() {
        // given
        GpxFile gpxFile = GpxFile.builder().id(1L).filename("test.gpx").build();

        Climb climb = Climb.builder()
                .id(1L)
                .gpxFile(gpxFile)
                .distanceMeters(500.0)
                .elevationGainMeters(50.0)
                .averageGradient(0.1)
                .startPointIndex(5)
                .endPointIndex(15)
                .build();

        // when
        ClimbResponse response = gpxAnalysisMapper.toClimbResponse(climb);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.distanceMeters()).isEqualTo(500.0);
        assertThat(response.elevationGainMeters()).isEqualTo(50.0);
        assertThat(response.averageGradient()).isEqualTo(0.1);
        assertThat(response.startPointIndex()).isEqualTo(5);
        assertThat(response.endPointIndex()).isEqualTo(15);
    }

    @Test
    void shouldMapGpxFileAndClimbsToGpxAnalysisResponse() {
        // given
        String gpxContent =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="Test">
                  <trk>
                    <name>Test Track</name>
                    <trkseg>
                      <trkpt lat="51.5000" lon="-0.1000"><ele>100</ele></trkpt>
                      <trkpt lat="51.5010" lon="-0.1000"><ele>110</ele></trkpt>
                      <trkpt lat="51.5020" lon="-0.1000"><ele>125</ele></trkpt>
                      <trkpt lat="51.5030" lon="-0.1000"><ele>140</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        GpxFile gpxFile = GpxFile.builder()
                .id(1L)
                .filename("test.gpx")
                .content(gpxContent)
                .userId(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Climb climb = Climb.builder()
                .id(1L)
                .gpxFile(gpxFile)
                .distanceMeters(200.0)
                .elevationGainMeters(40.0)
                .averageGradient(0.2)
                .startPointIndex(0)
                .endPointIndex(3)
                .build();

        // when
        GpxAnalysisResponse response = gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, List.of(climb));

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        assertThat(response.filename()).isEqualTo("test.gpx");
        assertThat(response.climbCount()).isEqualTo(1);
        assertThat(response.climbs()).hasSize(1);
        assertThat(response.totalDistanceKm()).isGreaterThan(0.0);
        assertThat(response.estimatedRideTimeMinutes()).isGreaterThan(0.0);
        assertThat(response.uploadedAt()).isNotNull();
    }

    @Test
    void shouldHandleGpxFileWithNoClimbs() {
        // given
        String gpxContent =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="Test">
                  <trk>
                    <name>Flat Track</name>
                    <trkseg>
                      <trkpt lat="51.5000" lon="-0.1000"><ele>100</ele></trkpt>
                      <trkpt lat="51.5010" lon="-0.1000"><ele>100</ele></trkpt>
                      <trkpt lat="51.5020" lon="-0.1000"><ele>100</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        GpxFile gpxFile = GpxFile.builder()
                .id(1L)
                .filename("flat.gpx")
                .content(gpxContent)
                .userId(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        GpxAnalysisResponse response = gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, List.of());

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        assertThat(response.filename()).isEqualTo("flat.gpx");
        assertThat(response.climbCount()).isEqualTo(0);
        assertThat(response.climbs()).isEmpty();
        assertThat(response.totalDistanceKm()).isGreaterThan(0.0);
        assertThat(response.estimatedRideTimeMinutes()).isGreaterThan(0.0);
    }

    @Test
    void shouldHandleInvalidGpxContent() {
        // given
        GpxFile gpxFile = GpxFile.builder()
                .id(1L)
                .filename("invalid.gpx")
                .content("invalid xml content")
                .userId(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        GpxAnalysisResponse response = gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, List.of());

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        assertThat(response.totalDistanceKm()).isEqualTo(0.0);
        assertThat(response.estimatedRideTimeMinutes()).isEqualTo(0.0);
    }
}
