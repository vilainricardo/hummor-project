package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> The resolved user id does not reference a persisted {@link com.rb.multi.agent.entity.Doctor} row.</p>
 * <p><b>PT-BR:</b> O id de utilizador não corresponde a um perfil {@link com.rb.multi.agent.entity.Doctor} persistido.</p>
 */
public final class NotDoctorProfileException extends RuntimeException {

	private final UUID userId;

	public NotDoctorProfileException(UUID userId) {
		super("user " + userId + " is not registered as a doctor profile");
		this.userId = userId;
	}

	public UUID getUserId() {
		return userId;
	}
}
