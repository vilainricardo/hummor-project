-- FR-006 domain / sleep self-assessment: value 0–10 (append-style event row per entry).

CREATE TABLE sleep_entries (
    id UUID NOT NULL PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    value SMALLINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sleep_value_range CHECK (value >= 0 AND value <= 10)
);

CREATE INDEX idx_sleep_entries_patient_created ON sleep_entries (patient_id, created_at DESC);

COMMENT ON TABLE sleep_entries IS 'Patient-reported sleep quality/agreement scale 0–10 (FR-006; product scale).';
