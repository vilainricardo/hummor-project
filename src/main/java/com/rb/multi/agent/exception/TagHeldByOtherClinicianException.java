package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Acting clinician cited a catalogue tag row already credited to another doctor for this patient.</p>
 * <p><b>PT-BR:</b> O clínico referiu uma etiqueta já atribuída por outro médico ao mesmo paciente.</p>
 */
public class TagHeldByOtherClinicianException extends RuntimeException {

	private final UUID catalogueTagId;

	public TagHeldByOtherClinicianException(UUID catalogueTagId) {
		super("catalogue tag already attributed by another clinician: " + catalogueTagId);
		this.catalogueTagId = catalogueTagId;
	}

	public UUID catalogueTagId() {
		return catalogueTagId;
	}
}
