-- FR-004: paciente define o início da partilha; persiste no vínculo pendente e na lista activa doctor_patients.

ALTER TABLE doctor_patient_mutual_links
    ADD COLUMN access_start_date DATE;

COMMENT ON COLUMN doctor_patient_mutual_links.access_start_date IS 'Set when the patient acknowledges; copied to doctor_patients on mutual completion (FR-004).';

ALTER TABLE doctor_patients
    ADD COLUMN access_start_date DATE NOT NULL DEFAULT CURRENT_DATE;

COMMENT ON COLUMN doctor_patients.access_start_date IS 'Inclusive start date for doctor access to patient data (FR-004); patient-defined at link finalization.';
