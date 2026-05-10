package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Patient reached the maximum number of distinct self-assigned catalogue tags.</p>
 * <p><b>PT-BR:</b> Limite de etiquetas de catálogo auto-atribuídas pelo paciente.</p>
 */
public class PatientSelfTagSliceFullException extends RuntimeException {

	private final UUID patientId;

	public PatientSelfTagSliceFullException(UUID patientId) {
		super("patient " + patientId + " reached self-tag assignment limit");
		this.patientId = patientId;
	}

	public UUID patientId() {
		return patientId;
	}
}
