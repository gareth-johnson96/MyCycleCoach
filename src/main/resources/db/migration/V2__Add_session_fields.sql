-- Add new fields to planned_sessions table
ALTER TABLE planned_sessions
    ADD COLUMN tss INT,
    ADD COLUMN elevation INT,
    ADD COLUMN target_zone VARCHAR(50);
