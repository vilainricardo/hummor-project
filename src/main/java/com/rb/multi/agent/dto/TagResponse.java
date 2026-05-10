package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.constants.TagCategory;

/**
 * <p><b>EN:</b> Read-model DTO for {@link Tag} responses. When projected from a clinician assignment, {@code critical}
 * and {@code assignedByClinicianId} are set; omitted from catalogue-only routes.</p>
 * <p><b>PT-BR:</b> DTO de tag; com atribuição médica preenchem-se criticidade e id do médico.</p>
 */
@JsonInclude(Include.NON_NULL)
public record TagResponse(
		UUID id,
		String name,
		String description,
		TagCategory category,
		Instant createdAt,
		Boolean criticalForClinician,
		UUID assignedByClinicianId
) {

	/** EN: Maps entity to outbound JSON. PT-BR: Converte entidade para JSON de saída. */
	public static TagResponse from(Tag entity) {
		return new TagResponse(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getCategory(),
				entity.getCreatedAt(),
				null,
				null);
	}

	/** EN: Tag plus per-clinician assignment metadata (user profile {@code tags} list). PT-BR: Tag com metadados do médico atribuidor. */
	public static TagResponse fromClinicianAssignment(Tag entity, boolean criticalForClinician, UUID assignedByClinicianId) {
		return new TagResponse(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getCategory(),
				entity.getCreatedAt(),
				criticalForClinician,
				assignedByClinicianId);
	}
}
