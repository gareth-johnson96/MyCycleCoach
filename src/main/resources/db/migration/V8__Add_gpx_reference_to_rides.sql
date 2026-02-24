-- Add gpx_file_id to rides table to link rides with their GPX data
ALTER TABLE rides ADD COLUMN gpx_file_id BIGINT;

-- Add additional Strava activity fields for more complete data mapping
ALTER TABLE rides ADD COLUMN sport_type VARCHAR(50);
ALTER TABLE rides ADD COLUMN workout_type INTEGER;
ALTER TABLE rides ADD COLUMN activity_type VARCHAR(50);

-- Create foreign key constraint
ALTER TABLE rides ADD CONSTRAINT fk_rides_gpx_file 
    FOREIGN KEY (gpx_file_id) REFERENCES gpx_files(id) ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX idx_rides_gpx_file_id ON rides(gpx_file_id);
