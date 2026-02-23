-- Add new fields to user_profiles table
ALTER TABLE user_profiles
    ADD COLUMN height DECIMAL(5,2),
    ADD COLUMN current_ftp INT,
    ADD COLUMN max_hr INT;

-- Add new fields to training_backgrounds table
ALTER TABLE training_backgrounds
    ADD COLUMN training_history TEXT,
    ADD COLUMN injury_history TEXT,
    ADD COLUMN daily_availability TEXT,
    ADD COLUMN weekly_training_times TEXT;

-- Add new fields to training_goals table
ALTER TABLE training_goals
    ADD COLUMN target_event VARCHAR(255),
    ADD COLUMN target_event_date TIMESTAMP;
