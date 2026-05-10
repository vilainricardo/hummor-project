package com.rb.multi.agent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.rb.multi.agent.dto.TagResponse;
import com.rb.multi.agent.dto.UserResponse;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.entity.UserTagAssignment;

/**
 * <p><b>EN:</b> Builds {@link UserResponse} including localized catalogue tag projections.</p>
 * <p><b>PT-BR:</b> Constrói {@link UserResponse} com tags do catálogo localizadas.</p>
 */
@Component
public class UserProfilePresenter {

	private final TagCatalogPresenter tagCatalogPresenter;

	public UserProfilePresenter(TagCatalogPresenter tagCatalogPresenter) {
		this.tagCatalogPresenter = tagCatalogPresenter;
	}

	/** EN: Maps aggregate to JSON read model. PT-BR: Mapeia agregado para modelo de leitura JSON. */
	public UserResponse present(User entity) {
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
										a -> a.getTag().getCode().toLowerCase(Locale.ROOT))
								.thenComparing(a -> a.getAssignedBy().getId()))
				.map(
						a ->
								tagCatalogPresenter.presentClinicianAssignment(
										a.getTag(), a.isCriticalForClinician(), a.getAssignedBy().getId()))
				.toList();
		List<TagResponse> selfList = uniqueSelfByTagId.values().stream()
				.sorted(Comparator.comparing(t -> t.getCode().toLowerCase(Locale.ROOT)))
				.map(tagCatalogPresenter::present)
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
