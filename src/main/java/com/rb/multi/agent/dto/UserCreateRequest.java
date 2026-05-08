package com.rb.multi.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Create-only payload — new accounts start with no catalogue tag links; associations use POST/DELETE
 * {@literal /api/v1/users/{id}/tag-assignments}.</p>
 * <p><b>PT-BR:</b> Payload só de criação — novas contas sem tags; associações via POST/DELETE em
 * {@literal /api/v1/users/{id}/tag-assignments}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UserCreateRequest(
		@NotBlank(message = "{validation.user.code.blank}")
		@Size(max = 20, message = "{validation.user.code.size}")
		String code,
		@NotBlank(message = "{validation.user.email.blank}")
		@Email(message = "{validation.user.email.format}")
		@Size(max = 320, message = "{validation.user.email.size}")
		String email,
		boolean doctor,
		Integer age,
		@Size(max = 100, message = "{validation.user.field.max}") String profession,
		@Size(max = 20, message = "{validation.user.field.max}") String postalCode,
		@Size(max = 100, message = "{validation.user.field.max}") String country,
		@Size(max = 100, message = "{validation.user.field.max}") String city,
		String addressLine,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		@Size(max = 128, message = "{validation.user.password.size}") String password) {

}
