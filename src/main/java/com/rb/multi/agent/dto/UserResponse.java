package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <p><b>EN:</b> Read-model DTO for {@link User} responses. Tag lists are built by
 * {@link com.rb.multi.agent.service.UserProfilePresenter} (localized catalogue strings).</p>
 * <p><b>PT-BR:</b> DTO de leitura; listas de tags via {@link com.rb.multi.agent.service.UserProfilePresenter}.</p>
 */
public record UserResponse(
		UUID id,
		String code,
		String email,
		boolean doctor,
		Integer age,
		String profession,
		String postalCode,
		String country,
		String city,
		String addressLine,
		Instant createdAt,
		List<TagResponse> tags,
		List<TagResponse> selfAssignedTags) {
}
