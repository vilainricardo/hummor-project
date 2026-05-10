package com.rb.multi.agent.dto;

import java.util.List;

import com.rb.multi.agent.constants.TagCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Upsert payload for {@link com.rb.multi.agent.entity.Tag}; {@code code} is immutable after create.</p>
 * <p><b>PT-BR:</b> Criar/atualizar tag; {@code code} não muda após a criação.</p>
 */
public record TagWriteRequest(
		@NotBlank(message = "{validation.tag.code.blank}")
		@Size(max = 64, message = "{validation.tag.code.size}")
		String code,
		@NotBlank(message = "{validation.tag.name.blank}")
		@Size(max = 50, message = "{validation.tag.name.size}")
		String name,
		@Size(max = 5000, message = "{validation.tag.description.size}")
		String description,
		@NotEmpty(message = "{validation.tag.categories.empty}")
		List<TagCategory> categories) {

}
