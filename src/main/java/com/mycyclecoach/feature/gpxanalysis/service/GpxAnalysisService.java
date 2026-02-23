package com.mycyclecoach.feature.gpxanalysis.service;

import com.mycyclecoach.feature.gpxanalysis.domain.GpxAnalysisResponse;
import org.springframework.web.multipart.MultipartFile;

public interface GpxAnalysisService {

    GpxAnalysisResponse analyzeGpxFile(MultipartFile file, Long userId);

    GpxAnalysisResponse getGpxAnalysis(Long gpxFileId);
}
