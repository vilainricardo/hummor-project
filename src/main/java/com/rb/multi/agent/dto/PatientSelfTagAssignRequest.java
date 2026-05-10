package com.rb.multi.agent.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;

/**
 * <p><b>EN:</b> Patient links one catalogue tag to their own profile ({@code assigned_by} = patient).</p>
 * <p><b>PT-BR:</b> Paciente liga uma tag do catálogo ao próprio perfil.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PatientSelfTagAssignRequest(
		@NotNull(message = "{validation.selfTag.tagId.required}")
		UUID tagId) {
}
