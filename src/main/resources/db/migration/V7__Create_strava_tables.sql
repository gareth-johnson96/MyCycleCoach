-- Strava connections table for storing OAuth tokens
CREATE TABLE strava_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    strava_athlete_id BIGINT NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    scope VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Rides table for storing activities from Strava
CREATE TABLE rides (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    strava_activity_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    distance DECIMAL(10,2),
    moving_time INT,
    elapsed_time INT,
    total_elevation_gain DECIMAL(10,2),
    start_date TIMESTAMP NOT NULL,
    average_speed DECIMAL(10,2),
    max_speed DECIMAL(10,2),
    average_watts DECIMAL(10,2),
    average_heartrate DECIMAL(10,2),
    max_heartrate DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_strava_connections_user_id ON strava_connections(user_id);
CREATE INDEX idx_strava_connections_strava_athlete_id ON strava_connections(strava_athlete_id);
CREATE INDEX idx_rides_user_id ON rides(user_id);
CREATE INDEX idx_rides_strava_activity_id ON rides(strava_activity_id);
CREATE INDEX idx_rides_start_date ON rides(start_date);
