package com.rb.multi.agent.exception;

import java.time.LocalDate;
import java.util.UUID;

/**
 * <p><b>EN:</b> A sleep score for this patient on the given calendar day already exists.</p>
 * <p><b>PT-BR:</b> Já existe registo de sono para este paciente neste dia.</p>
 */
public class SleepEntryDayConflictException extends RuntimeException {

	private final UUID patientId;
	private final LocalDate recordedOn;

	public SleepEntryDayConflictException(UUID patientId, LocalDate recordedOn) {
		super("sleep entry already exists for day");
		this.patientId = patientId;
		this.recordedOn = recordedOn;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public LocalDate getRecordedOn() {
		return recordedOn;
	}
}
