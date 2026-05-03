package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.entity.UserTagAssignment;

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

	/**
	 * <p><b>EN:</b> Maps entity to JSON; {@code tags} lists each catalogue tag once, only from clinician assignments.</p>
	 * <p><b>PT-BR:</b> Converte entidade para JSON; {@code tags} lista cada etiqueta do catálogo uma vez, só com atribuição por médico.</p>
	 */
	public static UserResponse from(User entity) {
		var uniqueByTagId = new LinkedHashMap<UUID, Tag>();
		for (UserTagAssignment a : entity.getTagAssignments()) {
			var assigner = a.getAssignedBy();
			if (assigner == null || !assigner.isDoctor()) {
				continue;
			}
			Tag tag = a.getTag();
			uniqueByTagId.putIfAbsent(tag.getId(), tag);
		}
		List<TagResponse> tagList = uniqueByTagId.values().stream()
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
