# Strava API Integration

## Overview

The MyCycleCoach application integrates with Strava API to allow users to:
- Connect their Strava accounts via OAuth2
- Automatically sync their cycling activities (rides)
- View synchronized ride data through the API

## Prerequisites

Before using the Strava integration, you need to:

1. Create a Strava API application at https://www.strava.com/settings/api
2. Note down your `Client ID` and `Client Secret`
3. Set the Authorization Callback Domain to your application domain (e.g., `localhost:8080` for local development)

## Configuration

### Required Environment Variables

```bash
STRAVA_CLIENT_ID=your_client_id_here
STRAVA_CLIENT_SECRET=your_client_secret_here
STRAVA_REDIRECT_URI=http://localhost:8080/api/v1/strava/callback
```

### Optional Configuration

The following can be configured in `application.yaml`:

```yaml
mycyclecoach:
  strava:
    sync:
      enabled: true  # Enable/disable scheduled sync
      cron: "0 0 */6 * * *"  # Sync every 6 hours by default
    token-refresh-buffer-seconds: 3600  # Refresh tokens 1 hour before expiration (default: 3600)
```

The `token-refresh-buffer-seconds` configuration ensures that tokens are refreshed proactively before they expire, preventing API call failures due to expired tokens. The default value of 3600 seconds (1 hour) provides a safe buffer for token refresh.

## User Flow

### 1. Connecting a Strava Account

**Step 1: Get Authorization URL**
```bash
GET /api/v1/strava/authorize
Authorization: Bearer <jwt_token>
```

Response:
```
https://www.strava.com/oauth/authorize?client_id=...&redirect_uri=...&response_type=code&scope=read,activity:read_all
```

**Step 2: Redirect User**
Redirect the user to the URL returned in step 1. They will authenticate with Strava and authorize your application.

**Step 3: Handle Callback**
After authorization, Strava redirects to your callback URL with an authorization code:
```
GET /api/v1/strava/callback?code=<authorization_code>&state=<jwt_token>
```

This endpoint automatically exchanges the code for access/refresh tokens and stores them.

### 2. Checking Connection Status

```bash
GET /api/v1/strava/connection
Authorization: Bearer <jwt_token>
```

Response:
```json
{
  "id": 1,
  "userId": 123,
  "stravaAthleteId": 456789,
  "connected": true,
  "connectedAt": "2024-01-15T10:30:00"
}
```

### 3. Syncing Rides

**Manual Sync**
```bash
POST /api/v1/strava/sync
Authorization: Bearer <jwt_token>
```

**Automatic Sync**
The system automatically syncs rides for all connected users every 6 hours (configurable).

### 4. Retrieving Synced Rides

```bash
GET /api/v1/strava/rides
Authorization: Bearer <jwt_token>
```

Response:
```json
[
  {
    "id": 1,
    "stravaActivityId": 12345678,
    "name": "Morning Ride",
    "distance": 25500.0,
    "movingTime": 3600,
    "elapsedTime": 3700,
    "totalElevationGain": 250.0,
    "startDate": "2024-01-15T08:00:00",
    "averageSpeed": 7.08,
    "maxSpeed": 12.5,
    "averageWatts": 180.0,
    "averageHeartrate": 145.0,
    "maxHeartrate": 175.0
  }
]
```

### 5. Disconnecting Strava

```bash
DELETE /api/v1/strava/connection
Authorization: Bearer <jwt_token>
```

This removes the connection and all associated tokens. Ride data is preserved.

## Database Schema

### strava_connections
Stores OAuth tokens and athlete information.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| user_id | BIGINT | Reference to users table (unique) |
| strava_athlete_id | BIGINT | Strava athlete ID |
| access_token | VARCHAR(255) | OAuth access token |
| refresh_token | VARCHAR(255) | OAuth refresh token |
| expires_at | TIMESTAMP | Token expiration time |
| scope | VARCHAR(255) | OAuth scopes granted |
| created_at | TIMESTAMP | Connection creation time |
| updated_at | TIMESTAMP | Last update time |

### rides
Stores synchronized activities from Strava.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| user_id | BIGINT | Reference to users table |
| strava_activity_id | BIGINT | Strava activity ID (unique) |
| name | VARCHAR(255) | Activity name |
| distance | DECIMAL(10,2) | Distance in meters |
| moving_time | INT | Moving time in seconds |
| elapsed_time | INT | Total elapsed time in seconds |
| total_elevation_gain | DECIMAL(10,2) | Elevation gain in meters |
| start_date | TIMESTAMP | Activity start time |
| average_speed | DECIMAL(10,2) | Average speed in m/s |
| max_speed | DECIMAL(10,2) | Max speed in m/s |
| average_watts | DECIMAL(10,2) | Average power output |
| average_heartrate | DECIMAL(10,2) | Average heart rate |
| max_heartrate | DECIMAL(10,2) | Maximum heart rate |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last update time |

## Security Considerations

1. **Token Storage**: Access and refresh tokens are stored securely in the database
2. **Automatic Refresh**: Tokens are automatically refreshed when they expire or are about to expire
3. **Proactive Refresh**: The system refreshes tokens before they expire (configurable buffer, default 1 hour) to prevent API call failures
4. **Scopes**: The application requests minimal scopes: `read` and `activity:read_all`
5. **User Isolation**: Each user can only access their own Strava data

## Error Handling

The API returns appropriate HTTP status codes:

- `200 OK` - Successful request
- `201 Created` - Resource created
- `202 Accepted` - Sync initiated
- `204 No Content` - Successful deletion
- `400 Bad Request` - Invalid request or OAuth error
- `404 Not Found` - Connection not found
- `502 Bad Gateway` - Strava API error

## Rate Limiting

Strava API has rate limits:
- 100 requests per 15 minutes
- 1000 requests per day

The application handles rate limiting gracefully and logs errors without failing user operations.

## Development

### Running Tests

```bash
./gradlew test --tests "com.mycyclecoach.feature.strava.*"
```

### Building

```bash
./gradlew build
```

### Local Development

1. Create a Strava API application
2. Set environment variables in `.env` file
3. Start the application: `./gradlew bootRun`
4. Access Swagger UI: http://localhost:8080/swagger-ui.html

## Troubleshooting

### "Invalid authorization code"
- The authorization code has already been used or expired
- User needs to re-authenticate

### "Token refresh failed"
- The refresh token is invalid or revoked
- User needs to disconnect and reconnect their Strava account

### "No activities synced"
- User may have no activities in the requested time period
- Check Strava API scopes are correctly granted

## Future Enhancements

Potential improvements:
- Webhook support for real-time activity updates
- Activity detail retrieval (streams, laps, splits)
- Support for other activity types (running, swimming)
- Activity analysis and insights
- Export functionality
