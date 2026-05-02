package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.UUID;

import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.TagCategory;

/**
 * <p><b>EN:</b> Read-model DTO for {@link Tag} responses.</p>
 * <p><b>PT-BR:</b> DTO de leitura para respostas de {@link Tag}.</p>
 */
public record TagResponse(
		UUID id,
		String name,
		String description,
		TagCategory category,
		Instant createdAt
) {

	/** EN: Maps entity to outbound JSON. PT-BR: Converte entidade para JSON de saída. */
	public static TagResponse from(Tag entity) {
		return new TagResponse(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getCategory(),
				entity.getCreatedAt());
	}
}
