package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.rb.multi.agent.constants.TagCategory;

/**
 * <p><b>EN:</b> Read-model DTO for {@link com.rb.multi.agent.entity.Tag}. {@code name} and {@code description} are the
 * locale-specific strings shown to users (resolved from {@code MessageSource} when keys exist).</p>
 * <p><b>PT-BR:</b> DTO de tag; {@code name} e {@code description} são as strings apresentadas (i18n quando existirem chaves).</p>
 */
@JsonInclude(Include.NON_NULL)
public record TagResponse(
		UUID id,
		String code,
		String name,
		String description,
		List<TagCategory> categories,
		Instant createdAt,
		Boolean criticalForClinician,
		UUID assignedByClinicianId) {
}
