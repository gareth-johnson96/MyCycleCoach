package com.mycyclecoach.feature.gpxanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mycyclecoach.feature.gpxanalysis.domain.*;
import com.mycyclecoach.feature.gpxanalysis.repository.ClimbRepository;
import com.mycyclecoach.feature.gpxanalysis.repository.GpxFileRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class GpxAnalysisServiceImplTest {

    @Mock
    private GpxFileRepository gpxFileRepository;

    @Mock
    private ClimbRepository climbRepository;

    @Mock
    private GpxAnalysisMapper gpxAnalysisMapper;

    @InjectMocks
    private GpxAnalysisServiceImpl gpxAnalysisService;

    @Test
    void shouldAnalyzeGpxFileSuccessfully() throws IOException {
        // given
        String gpxContent =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="Test">
                  <trk>
                    <name>Test Track</name>
                    <trkseg>
                      <trkpt lat="51.5000" lon="-0.1000"><ele>100</ele></trkpt>
                      <trkpt lat="51.5010" lon="-0.1000"><ele>120</ele></trkpt>
                      <trkpt lat="51.5020" lon="-0.1000"><ele>140</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        MockMultipartFile file =
                new MockMultipartFile("file", "test.gpx", "application/gpx+xml", gpxContent.getBytes());

        GpxFile savedGpxFile = GpxFile.builder()
                .id(1L)
                .filename("test.gpx")
                .content(gpxContent)
                .userId(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Climb climb = Climb.builder()
                .id(1L)
                .gpxFile(savedGpxFile)
                .distanceMeters(200.0)
                .elevationGainMeters(40.0)
                .averageGradient(0.2)
                .startPointIndex(0)
                .endPointIndex(2)
                .build();

        GpxAnalysisResponse expectedResponse = new GpxAnalysisResponse(
                1L,
                "test.gpx",
                1,
                List.of(new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 2)),
                1.5,
                5.0,
                LocalDateTime.now());

        given(gpxFileRepository.save(any(GpxFile.class))).willReturn(savedGpxFile);
        given(climbRepository.saveAll(anyList())).willReturn(List.of(climb));
        given(gpxAnalysisMapper.toGpxAnalysisResponse(any(GpxFile.class), anyList()))
                .willReturn(expectedResponse);

        // when
        GpxAnalysisResponse response = gpxAnalysisService.analyzeGpxFile(file, 100L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        assertThat(response.filename()).isEqualTo("test.gpx");
        then(gpxFileRepository).should().save(any(GpxFile.class));
        then(climbRepository).should().saveAll(anyList());
    }

    @Test
    void shouldThrowGpxParsingExceptionWhenFileIsInvalid() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.gpx", "application/gpx+xml", "invalid content".getBytes());

        GpxFile savedGpxFile = GpxFile.builder()
                .id(1L)
                .filename("test.gpx")
                .content("invalid content")
                .userId(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(gpxFileRepository.save(any(GpxFile.class))).willReturn(savedGpxFile);

        // when / then
        assertThatThrownBy(() -> gpxAnalysisService.analyzeGpxFile(file, 100L))
                .isInstanceOf(GpxParsingException.class)
                .hasMessageContaining("Failed to parse GPX file");
    }

    @Test
    void shouldGetGpxAnalysisSuccessfully() {
        // given
        GpxFile gpxFile = GpxFile.builder()
                .id(1L)
                .filename("test.gpx")
                .content("content")
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
                .endPointIndex(2)
                .build();

        GpxAnalysisResponse expectedResponse = new GpxAnalysisResponse(
                1L,
                "test.gpx",
                1,
                List.of(new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 2)),
                1.5,
                5.0,
                LocalDateTime.now());

        given(gpxFileRepository.findById(1L)).willReturn(Optional.of(gpxFile));
        given(climbRepository.findByGpxFileId(1L)).willReturn(List.of(climb));
        given(gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, List.of(climb))).willReturn(expectedResponse);

        // when
        GpxAnalysisResponse response = gpxAnalysisService.getGpxAnalysis(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        then(gpxFileRepository).should().findById(1L);
        then(climbRepository).should().findByGpxFileId(1L);
    }

    @Test
    void shouldThrowGpxFileNotFoundExceptionWhenIdIsInvalid() {
        // given
        given(gpxFileRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> gpxAnalysisService.getGpxAnalysis(99L))
                .isInstanceOf(GpxFileNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldAnalyzeByFilenameSuccessfully() {
        // given
        String filename = "test_ride.gpx";
        GpxFile gpxFile = GpxFile.builder()
                .id(1L)
                .filename(filename)
                .content("content")
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
                .endPointIndex(2)
                .build();

        GpxAnalysisResponse expectedResponse = new GpxAnalysisResponse(
                1L, filename, 1, List.of(new ClimbResponse(1L, 200.0, 40.0, 0.2, 0, 2)), 1.5, 5.0, LocalDateTime.now());

        given(gpxFileRepository.findByFilename(filename)).willReturn(Optional.of(gpxFile));
        given(climbRepository.findByGpxFileId(1L)).willReturn(List.of(climb));
        given(gpxAnalysisMapper.toGpxAnalysisResponse(gpxFile, List.of(climb))).willReturn(expectedResponse);

        // when
        GpxAnalysisResponse response = gpxAnalysisService.analyzeByFilename(filename);

        // then
        assertThat(response).isNotNull();
        assertThat(response.gpxFileId()).isEqualTo(1L);
        assertThat(response.filename()).isEqualTo(filename);
        then(gpxFileRepository).should().findByFilename(filename);
        then(climbRepository).should().findByGpxFileId(1L);
    }

    @Test
    void shouldThrowGpxFileNotFoundExceptionWhenFilenameIsInvalid() {
        // given
        String filename = "nonexistent.gpx";
        given(gpxFileRepository.findByFilename(filename)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> gpxAnalysisService.analyzeByFilename(filename))
                .isInstanceOf(GpxFileNotFoundException.class)
                .hasMessageContaining("GPX file not found with filename");
    }

    @Test
    void shouldReturnAllGpxFilesForUserSuccessfully() {
        // given
        Long userId = 100L;
        GpxFile gpxFile1 = GpxFile.builder()
                .id(1L)
                .filename("ride1.gpx")
                .content("gpx content 1")
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        GpxFile gpxFile2 = GpxFile.builder()
                .id(2L)
                .filename("ride2.gpx")
                .content("gpx content 2")
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(gpxFileRepository.findByUserId(userId)).willReturn(List.of(gpxFile1, gpxFile2));

        // when
        List<GpxFileResponse> result = gpxAnalysisService.getUserGpxFiles(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).filename()).isEqualTo("ride1.gpx");
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).filename()).isEqualTo("ride2.gpx");
        assertThat(result.get(1).userId()).isEqualTo(userId);

        then(gpxFileRepository).should().findByUserId(userId);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoGpxFiles() {
        // given
        Long userId = 999L;
        given(gpxFileRepository.findByUserId(userId)).willReturn(List.of());

        // when
        List<GpxFileResponse> result = gpxAnalysisService.getUserGpxFiles(userId);

        // then
        assertThat(result).isEmpty();
        then(gpxFileRepository).should().findByUserId(userId);
    }
}
