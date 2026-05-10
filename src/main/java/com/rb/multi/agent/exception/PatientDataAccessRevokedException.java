package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Roster status is {@link com.rb.multi.agent.constants.DoctorPatientLinkStatus#UNLINKED_WITHOUT_ACCESS} — patient revoked
 * clinician access (FR-003).</p>
 * <p><b>PT-BR:</b> Estado do vínculo é desvinculado sem acesso — paciente revogou o acesso do médico (FR-003).</p>
 */
public final class PatientDataAccessRevokedException extends RuntimeException {

	private final UUID doctorId;
	private final UUID patientId;

	public PatientDataAccessRevokedException(UUID doctorId, UUID patientId) {
		super("patient " + patientId + " revoked data access for doctor " + doctorId);
		this.doctorId = doctorId;
		this.patientId = patientId;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public UUID getPatientId() {
		return patientId;
	}
}
