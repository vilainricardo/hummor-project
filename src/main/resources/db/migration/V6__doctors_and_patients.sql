-- MindSignal API: JPA JOINED inheritance — doctors.id is the same as users.id; optional patient roster (M:N).

CREATE TABLE doctors (
    id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE doctor_patients (
    doctor_id UUID NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
    patient_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, patient_user_id)
);

CREATE INDEX idx_doctor_patients_patient ON doctor_patients (patient_user_id);
