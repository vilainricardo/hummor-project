package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> {@code assignedByDoctorId} does not resolve to a persisted {@link com.rb.multi.agent.entity.User} row.</p>
 * <p><b>PT-BR:</b> {@code assignedByDoctorId} não corresponde a um {@link com.rb.multi.agent.entity.User} persistido.</p>
 */
public class AssigningDoctorNotFoundException extends RuntimeException {

	private final UUID doctorId;

	public AssigningDoctorNotFoundException(UUID doctorId) {
		super("assigning doctor not found id=" + doctorId);
		this.doctorId = doctorId;
	}

	public UUID doctorId() {
		return doctorId;
	}

	public Object[] messageArguments() {
		return new Object[] { doctorId.toString() };
	}
}
