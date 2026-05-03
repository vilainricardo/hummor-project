package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.UnknownTagReferencesException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * EN: {@link UserService} integration tests covering lifecycle edge cases plus clinician→patient tagging flow.
 * PT-BR: Testes de integração do {@link UserService}, edge cases do ciclo de vida e fluxo de associação de tags pelo clínico.
 */
@SpringBootTest
@Transactional
class UserServiceIntTest {

	private static UserWriteRequest base(String code, boolean doctor, List<UUID> tagIds) {
		return new UserWriteRequest(code, doctor, null, null, null, null, null, null, tagIds);
	}

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TagRepository tagRepository;

	@BeforeEach
	void purge() {
		userRepository.deleteAll();
		tagRepository.deleteAll();
	}

	@Test
	@DisplayName("Fluxo: clínico atribui etiquetas ao utente paciente → leituras expõem só esse subconjunto do catálogo (nunca o catálogo inteiro)")
	void clinicianWritesTagIds_thenPatientReadShowsOnlyAuthorizedSubset() {
		var inCatalogUnused = tagRepository.save(new Tag("zzz-unused", null, TagCategory.OTHER));
		var tB = tagRepository.save(new Tag("released-b", null, TagCategory.SLEEP));
		var tA = tagRepository.save(new Tag("released-a", null, TagCategory.SLEEP));

		var savedPatient = userService.create(base("pac-901", false, List.of()));

		assertThat(userService.findById(savedPatient.getId()).orElseThrow().getTags()).isEmpty();
		assertThat(userService.findByCode("pac-901").orElseThrow().getTags()).isEmpty();

		/** Simulates the same atomic write model the clinician console would send: full replace {@code tagIds}. */
		userService.update(
				savedPatient.getId(),
				new UserWriteRequest("pac-901", false, null, null, null, null, null, null, List.of(tB.getId(), tA.getId())));

		User readViaCode = userService.findByCode("pac-901").orElseThrow();
		assertThat(readViaCode.isDoctor()).isFalse();
		assertThat(readViaCode.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder(tA.getName(), tB.getName());
		assertThat(readViaCode.getTags()).extracting(Tag::getId).doesNotContain(inCatalogUnused.getId());

		User readViaId = userService.findById(savedPatient.getId()).orElseThrow();
		assertThat(readViaId.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder(tA.getName(), tB.getName());

		userService.update(
				savedPatient.getId(),
				new UserWriteRequest("pac-901", false, null, null, null, null, null, null, List.of(tA.getId())));
		assertThat(userService.findById(savedPatient.getId()).orElseThrow().getTags())
				.extracting(Tag::getName)
				.containsExactly(tA.getName());
	}

	@Test
	@DisplayName("boolean doctor só descreve a conta; mesmo subconjunto de tagIds reaplicado não depende da flag médica para persistência")
	void doctorFlag_toggleDoesNotLeakExtraCatalogTagsWhenSubsetUnchanged() {
		var tagged = tagRepository.save(new Tag("role-proof", null, TagCategory.SLEEP));
		var u = userService.create(base("patient-role", false, List.of()));

		userService.update(
				u.getId(),
				new UserWriteRequest(
						"patient-role",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						List.of(tagged.getId())));
		userService.update(
				u.getId(),
				new UserWriteRequest(
						"patient-role",
						true,
						null,
						null,
						null,
						null,
						null,
						null,
						List.of(tagged.getId())));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).extracting(Tag::getId).containsExactly(tagged.getId());
	}

	@Test
	@DisplayName("update com tagIds vazio remove todas as associações libertadas antes visíveis")
	void update_clearTagIds_dropsAllAssociations() {
		var t = tagRepository.save(new Tag("solo", null, TagCategory.SLEEP));

		var u = userService.create(base("p-clear", false, List.of(t.getId())));
		assertThat(u.getTags()).hasSize(1);

		userService.update(u.getId(), base("p-clear", false, List.of()));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).isEmpty();
	}

	@Test
	@DisplayName("tagIds repetidos deduplica mantendo a primeira ocorrência (ordem lógica de substituição)")
	void update_duplicateTagUuidInPayload_deduplicatesKeepsStableMembership() {
		var tA = tagRepository.save(new Tag("dup-a", null, TagCategory.OTHER));
		var tB = tagRepository.save(new Tag("dup-b", null, TagCategory.OTHER));

		var u = userService.create(base("p-dup", false, List.of()));

		userService.update(
				u.getId(),
				new UserWriteRequest("p-dup", false, null, null, null, null, null, null, List.of(tA.getId(), tA.getId(), tB.getId())));

		var tags = userService.findById(u.getId()).orElseThrow().getTags();
		assertThat(tags).hasSize(2);
		assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("dup-a", "dup-b");
	}

	@Test
	@DisplayName("substituição integral: segunda escrita substitui o conjunto de tags pela nova lista")
	void update_secondWriteReplacesEntireSubset() {
		var tKeep = tagRepository.save(new Tag("keep-me", null, TagCategory.SLEEP));
		var old = tagRepository.save(new Tag("discard-me", null, TagCategory.SLEEP));

		var u = userService.create(base("p-sub", false, List.of(old.getId())));
		userService.update(u.getId(), base("p-sub", false, List.of(tKeep.getId())));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags())
				.extracting(Tag::getId)
				.containsExactly(tKeep.getId());
	}

	@Test
	@DisplayName("UUID desconhecido no catálogo → UnknownTagReferencesException com snapshot dos ids falhados")
	void create_unknownTagIds_throws() {
		assertThatThrownBy(() -> userService.create(base("p-bad-tags", false, List.of(UUID.randomUUID()))))
				.isInstanceOfSatisfying(
						UnknownTagReferencesException.class,
						ex -> assertThat(ex.getUnknownIds()).hasSize(1));
	}

	@Test
	@DisplayName("code público duplicado após normalização trim → DuplicateUserCodeException")
	void create_duplicateTrimmedCode_throwsConflict() {
		userService.create(base("  shared  ", false, List.of()));
		assertThatThrownBy(() -> userService.create(base("SHARED", false, List.of())))
				.isInstanceOf(DuplicateUserCodeException.class);
	}

	@Test
	@DisplayName("code só brancos / vazio normalizado falha antes de unicidade")
	void create_blankAfterTrim_throwsIllegalArgument() {
		assertThatThrownBy(() -> userService.create(base(" \t\r\n ", false, List.of()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("code com mais de 20 caracteres após strip → IllegalArgumentException")
	void create_codeTooLong_throwsIllegalArgument() {
		assertThatThrownBy(() -> userService.create(base("123456789012345678901", false, List.of())))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("findByCode localiza igualmente valores normalizados (trim)") 
	void findByCode_trimsConsistency() {
		var saved = userService.create(base("  trimmed-id  ", false, List.of()));
		assertThat(userService.findByCode("trimmed-id").orElseThrow().getId()).isEqualTo(saved.getId());
	}

	@Test
	@DisplayName("update utilizador inexistente → UserNotFoundException")
	void update_missingUser_throws() {
		assertThatThrownBy(
				() -> userService.update(
						UUID.randomUUID(),
						base("ghost", false, List.of())))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("update reusando code de terceiros → DuplicateUserCodeException")
	void update_conflictWithOtherPersistedCode_throws() {
		userService.create(base("alice", false, List.of()));
		var bob = userService.create(base("bob", false, List.of()));
		assertThatThrownBy(() -> userService.update(bob.getId(), base("alice", false, List.of())))
				.isInstanceOf(DuplicateUserCodeException.class);
	}

	@Test
	@DisplayName("update mantendo o próprio code e tags reinserindo lista idêntica → sem extra tags no conjunto")
	void update_repeatSameAssociationList_stable() {
		var ta = tagRepository.save(new Tag("stable-left", null, TagCategory.SLEEP));

		var u = userService.create(base("stable-u", false, List.of(ta.getId())));
		userService.update(u.getId(), base("stable-u", false, List.of(ta.getId())));

		var tags = userService.findById(u.getId()).orElseThrow().getTags().stream().map(Tag::getId).toList();

		assertThat(tags).containsExactly(ta.getId());
	}

	@Test
	@DisplayName("delete existe remove o agregado; delete id fantasma lança UserNotFoundException")
	void delete_edgeCases() {
		var catalogueRow = tagRepository.save(new Tag("alive", null, TagCategory.SLEEP));

		var u = userService.create(base("alive-code", false, List.of(catalogueRow.getId())));

		userService.deleteById(u.getId());
		assertThat(userRepository.existsById(u.getId())).isFalse();
		assertThat(tagRepository.existsById(catalogueRow.getId())).isTrue();

		assertThatThrownBy(() -> userService.deleteById(u.getId())).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("findAllWithTags hydrated evita regressão de lazy/forçar inicialização antes de ler tags")
	void findAll_includesAssociationsWithTags() {
		var t = tagRepository.save(new Tag("bulk", null, TagCategory.SLEEP));

		userService.create(base("l1", false, List.of(t.getId())));
		userService.create(base("l2", false, List.of()));

		assertThat(userService.findAll()).hasSize(2);
		userService.findAll().forEach(u -> assertThat(u.getTags()).isNotNull());
	}
}
