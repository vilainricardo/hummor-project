package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.UUID;

import com.rb.multi.agent.entity.User;

/** Representação REST de {@link User} em leitura. */
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
		Instant createdAt
) {

	public static UserResponse from(User entity) {
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
				entity.getCreatedAt());
	}
}
