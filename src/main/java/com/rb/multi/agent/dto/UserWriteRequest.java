package com.rb.multi.agent.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Upsert payload for {@link com.rb.multi.agent.entity.User}; trimming and {@code code} sizing also enforced in
 * {@link com.rb.multi.agent.service.UserService}. List {@code tagIds} replaces the whole association (empty ⇒ none).</p>
 * <p><b>PT-BR:</b> Payload de criar/atualizar {@link com.rb.multi.agent.entity.User}; trim e tamanho de {@code code} são
 * validados também no {@link com.rb.multi.agent.service.UserService}. A lista {@code tagIds} substitui toda a associação (vazio ⇒ sem tags).</p>
 */
public record UserWriteRequest(
		@NotBlank(message = "{validation.user.code.blank}")
		@Size(max = 20, message = "{validation.user.code.size}")
		String code,
		boolean doctor,
		Integer age,
		@Size(max = 100, message = "{validation.user.field.max}") String profession,
		@Size(max = 20, message = "{validation.user.field.max}") String postalCode,
		@Size(max = 100, message = "{validation.user.field.max}") String country,
		@Size(max = 100, message = "{validation.user.field.max}") String city,
		String addressLine,
		@Size(max = 100, message = "{validation.user.tagIds.size}")
		List<UUID> tagIds) {

	public UserWriteRequest {
		tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
	}
}
