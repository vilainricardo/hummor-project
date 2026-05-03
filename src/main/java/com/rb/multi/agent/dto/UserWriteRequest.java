package com.rb.multi.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Profile update payload (code, clinician flag, demographics). Catalogue-tag links are maintained via
 * {@code POST}/{@code DELETE} under {@link com.rb.multi.agent.controller.UserController#assignCatalogueTag}.</p>
 * <p><b>PT-BR:</b> Actualização só de perfil (code, flag médico, dados demográficos). Ligações a etiquetas de catálogo
 * via POST/DELETE em {@code /users/.../tag-assignments}.</p>
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
		String addressLine) {
}
