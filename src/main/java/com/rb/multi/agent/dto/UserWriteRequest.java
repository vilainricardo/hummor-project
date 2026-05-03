package com.rb.multi.agent.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Full replace payload for updates to {@link com.rb.multi.agent.entity.User}. New accounts use
 * {@link UserCreateRequest} (no {@code tagIds}). Field {@code tagIds} replaces {@strong only} the catalogue links
 * credited to {@code assignedByDoctorId}, up to five per update; assignments from other clinicians remain on the patient.</p>
 * <p><b>PT-BR:</b> Campo {@code tagIds} substitui apenas as etiquetas atribuídas por {@code assignedByDoctorId}
 * (máx. 5 por pedido); etiquetas de outros médicos permanecem.</p>
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
