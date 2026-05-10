-- FR-005 / mood self-assessment: value 0–10 (append-style row per entry).

CREATE TABLE mood_entries (
    id UUID NOT NULL PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    value SMALLINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mood_value_range CHECK (value >= 0 AND value <= 10)
);

CREATE INDEX idx_mood_entries_patient_created ON mood_entries (patient_id, created_at DESC);

COMMENT ON TABLE mood_entries IS 'Patient-reported mood scale 0–10 (FR-005; product scale).';
