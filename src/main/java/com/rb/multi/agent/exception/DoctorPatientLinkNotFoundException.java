package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> No roster row ties the doctor to the patient (mutual consent link never completed or wrong ids).</p>
 * <p><b>PT-BR:</b> Sem linha de lista médico–paciente (vínculo inexistente ou ids incorrectos).</p>
 */
public final class DoctorPatientLinkNotFoundException extends RuntimeException {

	private final UUID doctorId;
	private final UUID patientId;

	public DoctorPatientLinkNotFoundException(UUID doctorId, UUID patientId) {
		super("no doctor–patient roster row for doctor " + doctorId + " and patient " + patientId);
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
