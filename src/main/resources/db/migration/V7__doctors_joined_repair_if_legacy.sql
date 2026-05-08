-- Repairs databases that ran an older V6 shape (doctors with separate id + user_id). Idempotent-ish for Postgres.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'doctors'
          AND column_name = 'user_id'
    ) THEN
        DROP TABLE IF EXISTS doctor_patients CASCADE;
        DROP TABLE IF EXISTS doctors CASCADE;
        CREATE TABLE doctors (
            id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE
        );
        CREATE TABLE doctor_patients (
            doctor_id UUID NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
            patient_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
            PRIMARY KEY (doctor_id, patient_user_id)
        );
        CREATE INDEX idx_doctor_patients_patient ON doctor_patients (patient_user_id);
        INSERT INTO doctors (id)
        SELECT id FROM users WHERE is_doctor = TRUE;
    END IF;
END $$;
