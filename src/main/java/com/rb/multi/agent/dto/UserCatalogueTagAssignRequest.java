package com.rb.multi.agent.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * <p><b>EN:</b> Request body linking one catalogue {@code tag} to a patient for a given clinician; {@code critical}
 * marks clinical priority for that physician’s assignment row.</p>
 * <p><b>PT-BR:</b> Liga uma tag do catálogo ao paciente; {@code critical} indica prioridade/criticidade para esse médico.</p>
 */
public record UserCatalogueTagAssignRequest(
		@NotNull UUID assignedByDoctorId,
		@NotNull UUID tagId,
		boolean critical) {

	/** EN: Backward-compatible JSON omitting {@code critical} (defaults to false). PT-BR: Compatível com JSON sem {@code critical}. */
	public UserCatalogueTagAssignRequest(UUID assignedByDoctorId, UUID tagId) {
		this(assignedByDoctorId, tagId, false);
	}
}
