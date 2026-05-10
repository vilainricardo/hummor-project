package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Signals that a mood entry cannot be stored because another was recorded too recently.</p>
 * <p><b>PT-BR:</b> Não é possível gravar humor porque existe registo recente demais.</p>
 */
public class MoodEntryTooSoonException extends RuntimeException {

	private final UUID patientId;

	public MoodEntryTooSoonException(UUID patientId) {
		super("mood entry minimum interval not elapsed");
		this.patientId = patientId;
	}

	public UUID getPatientId() {
		return patientId;
	}
}
