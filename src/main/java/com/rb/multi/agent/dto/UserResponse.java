package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.ArrayList;
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

	/**
	 * <p><b>EN:</b> Maps entity to JSON; {@code tags} lists one entry per clinician assignment (same catalogue tag may appear
	 * twice if two doctors assign it), with {@link TagResponse#criticalForClinician()} when relevant.</p>
	 * <p><b>PT-BR:</b> {@code tags} = uma entrada por atribuição de médico; {@code selfAssignedTags} = auto-atribuições.</p>
	 */
	public static UserResponse from(User entity) {
		var uniqueSelfByTagId = new LinkedHashMap<UUID, Tag>();
		UUID selfId = entity.getId();
		ArrayList<UserTagAssignment> clinicianAssignments = new ArrayList<>();
		for (UserTagAssignment a : entity.getTagAssignments()) {
			var assigner = a.getAssignedBy();
			if (assigner == null) {
				continue;
			}
			Tag tag = a.getTag();
			if (assigner.getId().equals(selfId) && !assigner.isDoctor()) {
				uniqueSelfByTagId.putIfAbsent(tag.getId(), tag);
			} else if (assigner.isDoctor()) {
				clinicianAssignments.add(a);
			}
		}
		List<TagResponse> tagList = clinicianAssignments.stream()
				.sorted(
						Comparator.<UserTagAssignment, String>comparing(
										a -> a.getTag().getName().toLowerCase(Locale.ROOT))
								.thenComparing(a -> a.getAssignedBy().getId()))
				.map(
						a ->
								TagResponse.fromClinicianAssignment(
										a.getTag(), a.isCriticalForClinician(), a.getAssignedBy().getId()))
				.toList();
		List<TagResponse> selfList = uniqueSelfByTagId.values().stream()
				.sorted(Comparator.comparing(t -> t.getName().toLowerCase(Locale.ROOT)))
				.map(TagResponse::from)
				.toList();
		return new UserResponse(
				entity.getId(),
				entity.getCode(),
				entity.getEmail(),
				entity.isDoctor(),
				entity.getAge(),
				entity.getProfession(),
				entity.getPostalCode(),
				entity.getCountry(),
				entity.getCity(),
				entity.getAddressLine(),
				entity.getCreatedAt(),
				tagList,
				selfList);
	}
}
