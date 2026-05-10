-- Per physician–patient–tag row: whether the clinician marks this catalogue tag as critical for follow-up.

ALTER TABLE user_tag_assignments ADD COLUMN is_critical BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN user_tag_assignments.is_critical IS 'Clinician flags this assignment as critical for this patient (FR-style alerting); self-assignments stay false.';
