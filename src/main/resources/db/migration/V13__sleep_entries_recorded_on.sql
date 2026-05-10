-- One sleep self-assessment per patient per calendar day (UTC). Canonical day for FR-006 diary.

ALTER TABLE sleep_entries ADD COLUMN recorded_on DATE;

UPDATE sleep_entries SET recorded_on = CAST(created_at AS DATE);

ALTER TABLE sleep_entries ALTER COLUMN recorded_on SET NOT NULL;

CREATE UNIQUE INDEX uq_sleep_entries_patient_recorded_day ON sleep_entries (patient_id, recorded_on);

COMMENT ON COLUMN sleep_entries.recorded_on IS 'Calendar day (UTC) this rating refers to; at most one row per patient per day.';
