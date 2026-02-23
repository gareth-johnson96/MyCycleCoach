-- Create climbs table
CREATE TABLE climbs (
    id BIGSERIAL PRIMARY KEY,
    gpx_file_id BIGINT NOT NULL,
    distance_meters DOUBLE PRECISION NOT NULL,
    elevation_gain_meters DOUBLE PRECISION NOT NULL,
    average_gradient DOUBLE PRECISION NOT NULL,
    start_point_index INTEGER NOT NULL,
    end_point_index INTEGER NOT NULL,
    CONSTRAINT fk_climbs_gpx_file FOREIGN KEY (gpx_file_id) REFERENCES gpx_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_climbs_gpx_file_id ON climbs(gpx_file_id);
