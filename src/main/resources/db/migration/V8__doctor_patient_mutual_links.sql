-- Mutual consent: persist half-steps until patient and doctor confirm each other's public codes; then roster row in doctor_patients.

CREATE TABLE doctor_patient_mutual_links (
    id UUID PRIMARY KEY,
    patient_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    doctor_user_id UUID NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
    patient_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    doctor_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_mutual_doctor_patient UNIQUE (patient_user_id, doctor_user_id),
    CONSTRAINT ck_mutual_not_self CHECK (patient_user_id <> doctor_user_id)
);

CREATE INDEX idx_mutual_links_doctor ON doctor_patient_mutual_links (doctor_user_id);
CREATE INDEX idx_mutual_links_patient ON doctor_patient_mutual_links (patient_user_id);
