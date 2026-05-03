-- Allow the same catalogue tag for one patient under different clinicians (each clinician's slice is independent).
ALTER TABLE user_tag_assignments DROP CONSTRAINT uk_uta_patient_tag;
ALTER TABLE user_tag_assignments
    ADD CONSTRAINT uk_uta_patient_tag_assigner UNIQUE (patient_id, tag_id, assigned_by_user_id);
