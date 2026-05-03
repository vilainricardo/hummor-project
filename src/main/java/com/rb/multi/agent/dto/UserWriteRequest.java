package com.rb.multi.agent.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Full replace payload for updates to {@link com.rb.multi.agent.entity.User}. New accounts use
 * {@link UserCreateRequest} (no {@code tagIds}). List {@code tagIds} replaces assignments (empty ⇒ none).</p>
 * <p><b>PT-BR:</b> Payload de substituição total; se o conjunto de {@code tagIds} mudar relativamente ao persistido,
 * {@code assignedByDoctorId} tem de identificar um utilizador médico; paciente com no máximo cinco etiquetas atribuídas.</p>
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
		UUID assignedByDoctorId,
		@Size(max = 5, message = "{validation.user.tagIds.size}")
		List<UUID> tagIds) {

	public UserWriteRequest {
		tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
	}
}
