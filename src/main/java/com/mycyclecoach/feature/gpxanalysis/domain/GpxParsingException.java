package com.mycyclecoach.feature.gpxanalysis.domain;

public class GpxParsingException extends RuntimeException {
    public GpxParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public GpxParsingException(String message) {
        super(message);
    }
}
