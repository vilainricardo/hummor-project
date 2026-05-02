package com.rb.multi.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Upsert payload for {@link com.rb.multi.agent.entity.User}; trimming and {@code code} sizing also enforced in
 * {@link com.rb.multi.agent.service.UserService}.</p>
 * <p><b>PT-BR:</b> Payload de criar/atualizar {@link com.rb.multi.agent.entity.User}; trim e tamanho de {@code code} são
 * validados também no {@link com.rb.multi.agent.service.UserService}.</p>
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
		String addressLine
) {
}
