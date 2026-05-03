package com.rb.multi.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Create-only payload — new accounts start with no catalogue tag links; associations are set via
 * {@link UserWriteRequest} on update.</p>
 * <p><b>PT-BR:</b> Payload só de criação — novas contas sem tags ligadas; associações via {@link UserWriteRequest}
 * na actualização.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UserCreateRequest(
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
