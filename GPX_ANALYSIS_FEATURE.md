# GPX Analysis Feature

## Overview

The GPX Analysis feature allows users to upload GPS Exchange Format (GPX) files and automatically detect and analyze climbs within the route.

## Endpoints

### Upload and Analyze GPX File

**POST** `/api/v1/gpx/upload`

Upload a GPX file for analysis.

**Parameters:**
- `file` (multipart file, required): The GPX file to upload
- `userId` (integer, required): The ID of the user uploading the file

**Response:**
```json
{
  "gpxFileId": 1,
  "filename": "morning_ride.gpx",
  "climbCount": 3,
  "climbs": [
    {
      "id": 1,
      "distanceMeters": 523.4,
      "elevationGainMeters": 45.2,
      "averageGradient": 0.0864,
      "startPointIndex": 12,
      "endPointIndex": 28
    },
    {
      "id": 2,
      "distanceMeters": 312.1,
      "elevationGainMeters": 28.5,
      "averageGradient": 0.0913,
      "startPointIndex": 45,
      "endPointIndex": 58
    }
  ],
  "uploadedAt": "2026-02-23T21:00:00"
}
```

**Status Codes:**
- `201 Created`: GPX file uploaded and analyzed successfully
- `400 Bad Request`: Invalid GPX file format or parsing error
- `500 Internal Server Error`: Unexpected server error

### Get GPX Analysis

**GET** `/api/v1/gpx/{gpxFileId}`

Retrieve previously analyzed GPX file data.

**Parameters:**
- `gpxFileId` (path parameter, required): The ID of the GPX file

**Response:** Same as upload endpoint

**Status Codes:**
- `200 OK`: GPX analysis retrieved successfully
- `404 Not Found`: GPX file with the specified ID does not exist
- `500 Internal Server Error`: Unexpected server error

## Climb Detection Algorithm

The service uses the following criteria to detect climbs:

- **Minimum Elevation Gain**: 10 meters
- **Minimum Distance**: 100 meters
- **Gradient Threshold**: 2% (0.02)

### How It Works

1. The algorithm processes GPX waypoints sequentially
2. When it encounters a segment with gradient ≥ 2%, it marks the start of a potential climb
3. It continues accumulating distance and elevation while the gradient remains above threshold
4. When the gradient drops below threshold, it evaluates if the accumulated climb meets minimum criteria
5. If criteria are met, the climb is saved; otherwise, it's discarded
6. The process continues for the remaining waypoints

### Distance Calculation

Distance between waypoints is calculated using the Haversine formula, which accounts for the Earth's curvature:

```
a = sin²(Δlat/2) + cos(lat₁) × cos(lat₂) × sin²(Δlon/2)
c = 2 × atan2(√a, √(1−a))
distance = R × c
```

Where R = 6,371,000 meters (Earth's radius)

## Database Schema

### gpx_files Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| filename | VARCHAR(255) | Original filename |
| content | TEXT | Full GPX file content |
| user_id | BIGINT | ID of the user who uploaded |
| created_at | TIMESTAMP | Upload timestamp (auto) |
| updated_at | TIMESTAMP | Last update timestamp (auto) |

### climbs Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| gpx_file_id | BIGINT | Foreign key to gpx_files |
| distance_meters | DOUBLE PRECISION | Climb distance in meters |
| elevation_gain_meters | DOUBLE PRECISION | Total elevation gain |
| average_gradient | DOUBLE PRECISION | Average gradient (0.05 = 5%) |
| start_point_index | INTEGER | Index of first waypoint |
| end_point_index | INTEGER | Index of last waypoint |

## Example Usage

### Using cURL

```bash
# Upload a GPX file
curl -X POST http://localhost:8080/api/v1/gpx/upload \
  -F "file=@/path/to/ride.gpx" \
  -F "userId=1"

# Get analysis by ID
curl http://localhost:8080/api/v1/gpx/123
```

### Using the Test Script

A test script is provided at `/tmp/test-gpx-api.sh`:

```bash
chmod +x /tmp/test-gpx-api.sh
./tmp/test-gpx-api.sh
```

## Sample GPX File

A sample GPX file with multiple climbs is provided at `/tmp/sample_ride.gpx` for testing purposes.

## Error Handling

### GpxParsingException (400 Bad Request)

Thrown when:
- GPX file format is invalid
- GPX file contains no waypoints
- GPX file cannot be read

Example response:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Failed to parse GPX file",
  "path": "/api/v1/gpx/upload",
  "timestamp": "2026-02-23T21:00:00"
}
```

### GpxFileNotFoundException (404 Not Found)

Thrown when attempting to retrieve a non-existent GPX file:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "GPX file not found with id: 999",
  "path": "/api/v1/gpx/999",
  "timestamp": "2026-02-23T21:00:00"
}
```

## Dependencies

- **io.jenetics:jpx** (version 3.0.0): GPX file parsing library
  - No known security vulnerabilities
  - Thread-safe and immutable data structures
  - Functional programming style with Stream API support

## Testing

The feature includes comprehensive tests:

### Unit Tests
- `GpxAnalysisServiceImplTest`: Tests service layer logic including climb detection
- Coverage: Service layer methods, error handling, edge cases

### Controller Tests
- `GpxAnalysisControllerTest`: Tests REST endpoints with mocked service
- Coverage: HTTP status codes, request/response mapping

### Running Tests

```bash
# Run all GPX analysis tests
./gradlew test --tests "com.mycyclecoach.feature.gpxanalysis.*"

# Run full test suite
./gradlew test

# Run with coverage report
./gradlew build
# Report available at: build/reports/jacoco/test/html/index.html
```

## Future Enhancements

Potential improvements for future iterations:

1. **Advanced Climb Categorization**: Categorize climbs by difficulty (HC, Cat 1-4)
2. **VAM (Vertical Ascent Meters)**: Calculate climbing speed metrics
3. **Segment Matching**: Match climbs against known Strava segments
4. **Descent Analysis**: Analyze descents for safety metrics
5. **Performance Metrics**: Calculate power estimates and effort scores
6. **Batch Upload**: Support multiple GPX files in a single request
7. **User Preferences**: Allow users to customize climb detection thresholds
8. **Export Formats**: Export analysis as PDF or CSV
