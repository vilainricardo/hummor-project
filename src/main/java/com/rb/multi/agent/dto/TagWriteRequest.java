package com.rb.multi.agent.dto;

import com.rb.multi.agent.entity.TagCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Upsert payload for {@link com.rb.multi.agent.entity.Tag}.</p>
 * <p><b>PT-BR:</b> Payload de criar/atualizar {@link com.rb.multi.agent.entity.Tag}.</p>
 */
public record TagWriteRequest(
		@NotBlank(message = "{validation.tag.name.blank}")
		@Size(max = 50, message = "{validation.tag.name.size}")
		String name,
		@Size(max = 5000, message = "{validation.tag.description.size}")
		String description,
		@NotNull(message = "{validation.tag.category.required}")
		TagCategory category
) {

}
