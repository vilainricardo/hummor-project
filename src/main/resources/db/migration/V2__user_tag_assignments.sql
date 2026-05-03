-- Replace bare user_tags join with audited clinician assignments (patient, tag, assigned_by, assigned_at).

DROP TABLE IF EXISTS user_tags;

CREATE TABLE user_tag_assignments (
    id UUID NOT NULL PRIMARY KEY,
    patient_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_uta_patient_tag UNIQUE (patient_id, tag_id),
    CONSTRAINT fk_uta_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uta_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    CONSTRAINT fk_uta_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_uta_tag_id ON user_tag_assignments (tag_id);
CREATE INDEX idx_uta_doctor ON user_tag_assignments (assigned_by_user_id);
