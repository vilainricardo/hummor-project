package com.rb.multi.agent.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * <p><b>EN:</b> Request body linking one catalogue {@code tag} to a patient for a given clinician.</p>
 * <p><b>PT-BR:</b> Corpo que liga uma {@code tag} do catálogo ao paciente em nome do médico.</p>
 */
public record UserCatalogueTagAssignRequest(@NotNull UUID assignedByDoctorId, @NotNull UUID tagId) {
}
