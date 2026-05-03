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

import com.rb.multi.agent.dto.UserCreateRequest;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.AssigningActorNotDoctorException;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.TagAssignmentDoctorRequiredException;
import com.rb.multi.agent.exception.TagAssignmentPatientOnlyException;
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

	private static UserCreateRequest createBase(String code, boolean doctor) {
		return new UserCreateRequest(code, doctor, null, null, null, null, null, null);
	}

	private static UserWriteRequest write(String code, boolean doctor, List<UUID> tagIds, UUID assignedByDoctorId) {
		return new UserWriteRequest(code, doctor, null, null, null, null, null, null, assignedByDoctorId, tagIds);
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
		var doc = userRepository.save(new User("doc-pac901", true));

		var savedPatient = userService.create(createBase("pac-901", false));

		assertThat(userService.findById(savedPatient.getId()).orElseThrow().getTags()).isEmpty();
		assertThat(userService.findByCode("pac-901").orElseThrow().getTags()).isEmpty();

		userService.update(
				savedPatient.getId(),
				write("pac-901", false, List.of(tB.getId(), tA.getId()), doc.getId()));

		User readViaCode = userService.findByCode("pac-901").orElseThrow();
		assertThat(readViaCode.isDoctor()).isFalse();
		assertThat(readViaCode.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder(tA.getName(), tB.getName());
		assertThat(readViaCode.getTags()).extracting(Tag::getId).doesNotContain(inCatalogUnused.getId());

		User readViaId = userService.findById(savedPatient.getId()).orElseThrow();
		assertThat(readViaId.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder(tA.getName(), tB.getName());

		userService.update(savedPatient.getId(), write("pac-901", false, List.of(tA.getId()), doc.getId()));
		assertThat(userService.findById(savedPatient.getId()).orElseThrow().getTags())
				.extracting(Tag::getName)
				.containsExactly(tA.getName());
	}

	@Test
	@DisplayName("boolean doctor só descreve a conta; mesmo subconjunto de tagIds reaplicado não depende da flag médica para persistência")
	void doctorFlag_toggleDoesNotLeakExtraCatalogTagsWhenSubsetUnchanged() {
		var tagged = tagRepository.save(new Tag("role-proof", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-role-proof", true));
		var u = userService.create(createBase("patient-role", false));

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
						doc.getId(),
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
						null,
						List.of(tagged.getId())));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).extracting(Tag::getId).containsExactly(tagged.getId());
	}

	@Test
	@DisplayName("update com tagIds vazio remove todas as associações libertadas antes visíveis")
	void update_clearTagIds_dropsAllAssociations() {
		var t = tagRepository.save(new Tag("solo", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-clear", true));

		var u = userService.create(createBase("p-clear", false));
		assertThat(u.getTags()).isEmpty();

		userService.update(u.getId(), write("p-clear", false, List.of(t.getId()), doc.getId()));
		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).hasSize(1);

		userService.update(u.getId(), write("p-clear", false, List.of(), doc.getId()));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).isEmpty();
	}

	@Test
	@DisplayName("tagIds repetidos deduplica mantendo a primeira ocorrência (ordem lógica de substituição)")
	void update_duplicateTagUuidInPayload_deduplicatesKeepsStableMembership() {
		var tA = tagRepository.save(new Tag("dup-a", null, TagCategory.OTHER));
		var tB = tagRepository.save(new Tag("dup-b", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-dup", true));

		var u = userService.create(createBase("p-dup", false));

		userService.update(
				u.getId(),
				write("p-dup", false, List.of(tA.getId(), tA.getId(), tB.getId()), doc.getId()));

		var tags = userService.findById(u.getId()).orElseThrow().getTags();
		assertThat(tags).hasSize(2);
		assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("dup-a", "dup-b");
	}

	@Test
	@DisplayName("alteração de tagIds sem assignedByDoctorId → TagAssignmentDoctorRequiredException")
	void update_tagsChangeWithoutDoctorId_throws() {
		var tagged = tagRepository.save(new Tag("need-doc", null, TagCategory.OTHER));
		var patient = userService.create(createBase("p-no-doc", false));
		assertThatThrownBy(() -> userService.update(patient.getId(), write("p-no-doc", false, List.of(tagged.getId()), null)))
				.isInstanceOf(TagAssignmentDoctorRequiredException.class);
	}

	@Test
	@DisplayName("assignedByDoctorId refere paciente não-médico → AssigningActorNotDoctorException")
	void update_actorNotDoctor_throws() {
		var tagged = tagRepository.save(new Tag("need-real-doc", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-real", true));
		var faker = userRepository.save(new User("fake-doc", false));
		var patient = userService.create(createBase("p-actor", false));
		userService.update(patient.getId(), write("p-actor", false, List.of(tagged.getId()), doc.getId()));

		assertThatThrownBy(() -> userService.update(patient.getId(), write("p-actor", false, List.of(), faker.getId())))
				.isInstanceOf(AssigningActorNotDoctorException.class);
	}

	@Test
	@DisplayName("conta médico não recebe tags de catálogo → TagAssignmentPatientOnlyException")
	void update_targetIsDoctor_throws() {
		var tagged = tagRepository.save(new Tag("clin-no-tags", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-target", true));
		var clinician = userService.create(createBase("clin-a", true));
		assertThatThrownBy(() -> userService.update(clinician.getId(), write("clin-a", true, List.of(tagged.getId()), doc.getId())))
				.isInstanceOf(TagAssignmentPatientOnlyException.class);
	}

	@Test
	@DisplayName("substituição integral: segunda escrita substitui o conjunto de tags pela nova lista")
	void update_secondWriteReplacesEntireSubset() {
		var tKeep = tagRepository.save(new Tag("keep-me", null, TagCategory.SLEEP));
		var old = tagRepository.save(new Tag("discard-me", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-sub", true));

		var u = userService.create(createBase("p-sub", false));
		userService.update(u.getId(), write("p-sub", false, List.of(old.getId()), doc.getId()));
		userService.update(u.getId(), write("p-sub", false, List.of(tKeep.getId()), doc.getId()));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags())
				.extracting(Tag::getId)
				.containsExactly(tKeep.getId());
	}

	@Test
	@DisplayName("update com UUID de tag inexistente → UnknownTagReferencesException")
	void update_unknownTagIds_throws() {
		var doc = userRepository.save(new User("doc-bad-tags", true));
		var u = userService.create(createBase("p-bad-tags", false));
		assertThatThrownBy(
						() -> userService.update(
								u.getId(), write("p-bad-tags", false, List.of(UUID.randomUUID()), doc.getId())))
				.isInstanceOfSatisfying(
						UnknownTagReferencesException.class,
						ex -> assertThat(ex.getUnknownIds()).hasSize(1));
	}

	@Test
	@DisplayName("code público duplicado após normalização trim → DuplicateUserCodeException")
	void create_duplicateTrimmedCode_throwsConflict() {
		userService.create(createBase("  shared  ", false));
		assertThatThrownBy(() -> userService.create(createBase("shared", false)))
				.isInstanceOf(DuplicateUserCodeException.class);
	}

	@Test
	@DisplayName("code só brancos / vazio normalizado falha antes de unicidade")
	void create_blankAfterTrim_throwsIllegalArgument() {
		assertThatThrownBy(() -> userService.create(createBase(" \t\r\n ", false))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("code com mais de 20 caracteres após strip → IllegalArgumentException")
	void create_codeTooLong_throwsIllegalArgument() {
		assertThatThrownBy(() -> userService.create(createBase("123456789012345678901", false)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("findByCode localiza igualmente valores normalizados (trim)")
	void findByCode_trimsConsistency() {
		var saved = userService.create(createBase("  trimmed-id  ", false));
		assertThat(userService.findByCode("trimmed-id").orElseThrow().getId()).isEqualTo(saved.getId());
	}

	@Test
	@DisplayName("update utilizador inexistente → UserNotFoundException")
	void update_missingUser_throws() {
		assertThatThrownBy(
				() -> userService.update(
						UUID.randomUUID(),
						write("ghost", false, List.of(), null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("update reusando code de terceiros → DuplicateUserCodeException")
	void update_conflictWithOtherPersistedCode_throws() {
		userService.create(createBase("alice", false));
		var bob = userService.create(createBase("bob", false));
		assertThatThrownBy(() -> userService.update(bob.getId(), write("alice", false, List.of(), null)))
				.isInstanceOf(DuplicateUserCodeException.class);
	}

	@Test
	@DisplayName("update mantendo o próprio code e tags reinserindo lista idêntica → sem extra tags no conjunto")
	void update_repeatSameAssociationList_stable() {
		var ta = tagRepository.save(new Tag("stable-left", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-stable", true));

		var u = userService.create(createBase("stable-u", false));
		userService.update(u.getId(), write("stable-u", false, List.of(ta.getId()), doc.getId()));
		userService.update(u.getId(), write("stable-u", false, List.of(ta.getId()), null));

		var tags = userService.findById(u.getId()).orElseThrow().getTags().stream().map(Tag::getId).toList();

		assertThat(tags).containsExactly(ta.getId());
	}

	@Test
	@DisplayName("delete existe remove o agregado; delete id fantasma lança UserNotFoundException")
	void delete_edgeCases() {
		var catalogueRow = tagRepository.save(new Tag("alive", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-alive", true));

		var u = userService.create(createBase("alive-code", false));
		userService.update(u.getId(), write("alive-code", false, List.of(catalogueRow.getId()), doc.getId()));

		userService.deleteById(u.getId());
		assertThat(userRepository.existsById(u.getId())).isFalse();
		assertThat(tagRepository.existsById(catalogueRow.getId())).isTrue();

		assertThatThrownBy(() -> userService.deleteById(u.getId())).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("findAllWithTags hydrated evita regressão de lazy/forçar inicialização antes de ler tags")
	void findAll_includesAssociationsWithTags() {
		var t = tagRepository.save(new Tag("bulk", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-bulk", true));

		var u1 = userService.create(createBase("l1", false));
		userService.update(u1.getId(), write("l1", false, List.of(t.getId()), doc.getId()));
		userService.create(createBase("l2", false));

		assertThat(userService.findAll()).hasSize(3);
		userService.findAll().forEach(u -> assertThat(u.getTags()).isNotNull());
	}
}
