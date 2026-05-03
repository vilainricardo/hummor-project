package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Clinician reached the ceiling of distinct catalogue-tag links for this patient (five per clinician).</p>
 * <p><b>PT-BR:</b> O médico atingiu o teto de etiquetas de catálogo distintas para este paciente (cinco por médico).</p>
 */
public class TagAssignmentSliceFullException extends RuntimeException {

	private final UUID patientId;
	private final UUID assignedByDoctorId;

	public TagAssignmentSliceFullException(UUID patientId, UUID assignedByDoctorId) {
		super(
				"clinician " + assignedByDoctorId + " already assigned 5 distinct catalogue tags to patient " + patientId);
		this.patientId = patientId;
		this.assignedByDoctorId = assignedByDoctorId;
	}

	public UUID patientId() {
		return patientId;
	}

	public UUID assignedByDoctorId() {
		return assignedByDoctorId;
	}
}
