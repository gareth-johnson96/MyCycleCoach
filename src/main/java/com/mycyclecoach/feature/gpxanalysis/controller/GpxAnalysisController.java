package com.mycyclecoach.feature.gpxanalysis.controller;

import com.mycyclecoach.feature.gpxanalysis.domain.GpxAnalysisResponse;
import com.mycyclecoach.feature.gpxanalysis.service.GpxAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/gpx")
@RequiredArgsConstructor
@Tag(name = "GPX Analysis", description = "GPX file upload and climb analysis endpoints")
public class GpxAnalysisController {

    private final GpxAnalysisService gpxAnalysisService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload and analyze a GPX file")
    @ApiResponse(responseCode = "201", description = "GPX file uploaded and analyzed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid GPX file format")
    public GpxAnalysisResponse uploadGpxFile(
            @RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
        return gpxAnalysisService.analyzeGpxFile(file, userId);
    }

    @GetMapping("/{gpxFileId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get GPX analysis by ID")
    @ApiResponse(responseCode = "404", description = "GPX file not found")
    public GpxAnalysisResponse getGpxAnalysis(@PathVariable Long gpxFileId) {
        return gpxAnalysisService.getGpxAnalysis(gpxFileId);
    }
}
