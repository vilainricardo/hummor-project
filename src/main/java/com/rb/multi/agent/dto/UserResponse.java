package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.rb.multi.agent.entity.User;

/**
 * <p><b>EN:</b> Read-model DTO for {@link User} responses.</p>
 * <p><b>PT-BR:</b> DTO de leitura para respostas de {@link User}.</p>
 */
public record UserResponse(
		UUID id,
		String code,
		boolean doctor,
		Integer age,
		String profession,
		String postalCode,
		String country,
		String city,
		String addressLine,
		Instant createdAt,
		List<TagResponse> tags) {

	/** EN: Maps entity to outbound JSON. PT-BR: Converte entidade para JSON de saída. */
	public static UserResponse from(User entity) {
		List<TagResponse> tagList = entity.getTags().stream()
				.sorted(Comparator.comparing(t -> t.getName().toLowerCase(Locale.ROOT)))
				.map(TagResponse::from)
				.toList();
		return new UserResponse(
				entity.getId(),
				entity.getCode(),
				entity.isDoctor(),
				entity.getAge(),
				entity.getProfession(),
				entity.getPostalCode(),
				entity.getCountry(),
				entity.getCity(),
				entity.getAddressLine(),
				entity.getCreatedAt(),
				tagList);
	}
}
