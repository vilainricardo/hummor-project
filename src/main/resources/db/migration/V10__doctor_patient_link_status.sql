-- FR-003: estado do vínculo médico–paciente na lista (doctor_patients).

ALTER TABLE doctor_patients
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';

COMMENT ON COLUMN doctor_patients.status IS 'FR-003: ACTIVE, UNLINKED_WITH_ACCESS, UNLINKED_WITHOUT_ACCESS';
