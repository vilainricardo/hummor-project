package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> No {@code user_tag_assignments} row for this patient/tag/clinician trio.</p>
 * <p><b>PT-BR:</b> Sem linha em {@code user_tag_assignments} para este trio paciente/tag/médico.</p>
 */
public class PatientTagAssignmentNotFoundException extends RuntimeException {

	private final UUID patientId;
	private final UUID assignedByDoctorId;
	private final UUID tagId;

	public PatientTagAssignmentNotFoundException(UUID patientId, UUID assignedByDoctorId, UUID tagId) {
		super("no catalogue tag assignment for patient " + patientId + ", doctor " + assignedByDoctorId + ", tag " + tagId);
		this.patientId = patientId;
		this.assignedByDoctorId = assignedByDoctorId;
		this.tagId = tagId;
	}

	public UUID patientId() {
		return patientId;
	}

	public UUID assignedByDoctorId() {
		return assignedByDoctorId;
	}

	public UUID tagId() {
		return tagId;
	}
}
