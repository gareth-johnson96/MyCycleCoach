package com.mycyclecoach.feature.gpxanalysis.domain;

public class GpxFileNotFoundException extends RuntimeException {
    public GpxFileNotFoundException(Long id) {
        super("GPX file not found with id: " + id);
    }
}
