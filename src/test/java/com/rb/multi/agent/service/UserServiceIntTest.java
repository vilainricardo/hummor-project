package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

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
import com.rb.multi.agent.exception.AssigningDoctorNotFoundException;
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

	@Autowired
	private Validator jakartaValidator;

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
	@DisplayName("slice: tagIds vazio + médico ⇒ remove só as etiquetas daquele clínico (único médico ⇒ paciente fica sem tags)")
	void update_clearTagIds_whenSoleClearingDoctor_dropsAssignments() {
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
	@DisplayName("slice: médico A limpa a sua lista; etiquetas mantidas pelo médico B permanecem no paciente")
	void update_clearOneClinicianSlice_preservesAssignmentsFromPeers() {
		var tagAonly = tagRepository.save(new Tag("solo-a-own", null, TagCategory.SLEEP));
		var tagBonly = tagRepository.save(new Tag("solo-b-own", null, TagCategory.SLEEP));
		var docA = userRepository.save(new User("doc-a-slice", true));
		var docB = userRepository.save(new User("doc-b-slice", true));

		var u = userService.create(createBase("p-slices", false));
		userService.update(u.getId(), write("p-slices", false, List.of(tagAonly.getId()), docA.getId()));
		userService.update(u.getId(), write("p-slices", false, List.of(tagBonly.getId()), docB.getId()));
		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).hasSize(2);

		userService.update(u.getId(), write("p-slices", false, List.of(), docA.getId()));

		User readBack = userService.findById(u.getId()).orElseThrow();
		assertThat(readBack.getTags()).hasSize(1);
		assertThat(readBack.getTags()).extracting(Tag::getId).containsExactly(tagBonly.getId());
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
	@DisplayName("assignedByDoctorId resolve paciente sem perfil médico e há mutação na fatia ⇒ AssigningActorNotDoctorException")
	void update_actorNotDoctor_throws() {
		var tagged = tagRepository.save(new Tag("need-real-doc", null, TagCategory.OTHER));
		var extraForFaker = tagRepository.save(new Tag("faker-cant-act", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-real", true));
		var faker = userRepository.save(new User("fake-doc", false));
		var patient = userService.create(createBase("p-actor", false));
		userService.update(patient.getId(), write("p-actor", false, List.of(tagged.getId()), doc.getId()));

		assertThatThrownBy(
						() -> userService.update(
								patient.getId(),
								write("p-actor", false, List.of(extraForFaker.getId()), faker.getId())))
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

	private List<Tag> saveTags(String prefix, int count, TagCategory category) {
		List<Tag> out = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			out.add(tagRepository.save(new Tag(prefix + "-" + i, null, category)));
		}
		return out;
	}

	@Test
	@DisplayName("borda: assignedByDoctorId inexistente e tagIds não vazio ⇒ AssigningDoctorNotFoundException")
	void edge_assigningDoctorUnknown_throws() {
		var t = tagRepository.save(new Tag("edge-ghost-doc", null, TagCategory.OTHER));
		var phantomTag = tagRepository.save(new Tag("edge-ghost-new", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-edge-ghost", true));
		var p = userService.create(createBase("p-ghost-assigner", false));
		userService.update(p.getId(), write("p-ghost-assigner", false, List.of(t.getId()), doc.getId()));
		UUID stranger = UUID.randomUUID();
		assertThatThrownBy(
						() -> userService.update(
								p.getId(),
								write(
										"p-ghost-assigner",
										false,
										List.of(phantomTag.getId()),
										stranger)))
				.isInstanceOfSatisfying(
						AssigningDoctorNotFoundException.class,
						ex -> assertThat(ex.doctorId()).isEqualTo(stranger));
	}

	@Test
	@DisplayName("borda: mistura de tagIds válidos e inexistentes devolve apenas os desconhecidos na excepção")
	void edge_knownAndUnknownTagIds_throwsListingUnknownOnly() {
		var catalog = tagRepository.save(new Tag("edge-known", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-mixed-tags", true));
		var patient = userService.create(createBase("p-mixed-tags", false));
		UUID phantomA = UUID.randomUUID();
		UUID phantomB = UUID.randomUUID();
		assertThatThrownBy(
						() -> userService.update(
								patient.getId(),
								write(
										"p-mixed-tags",
										false,
										List.of(catalog.getId(), phantomA, phantomB),
										doc.getId())))
				.isInstanceOfSatisfying(
						UnknownTagReferencesException.class,
						ex -> {
							assertThat(ex.getUnknownIds()).containsExactlyInAnyOrder(phantomA, phantomB);
						});
	}

	@Test
	@DisplayName("borda: até cinco etiquetas distintas por pedido/manifestação por médico (slice desse médico)")
	void edge_exactlyFiveDistinctTagIds_perClinicianUpdate_accepted() {
		var tags = saveTags("five", 5, TagCategory.OTHER);
		var doc = userRepository.save(new User("doc-five-cap", true));
		var ids = tags.stream().map(Tag::getId).toList();
		var patient = userService.create(createBase("p-five-cap", false));
		userService.update(patient.getId(), write("p-five-cap", false, ids, doc.getId()));
		assertThat(userService.findById(patient.getId()).orElseThrow().getTags()).hasSize(5);
	}

	@Test
	@DisplayName("vários médicos: cada um até 5 distintos ⇒ paciente acumula a união (ex.: dois médicos ⇒ até 10 tags)")
	void aggregation_twoDoctorsFiveEach_patientShowsTenDistinctTags() {
		var batchA = saveTags("mA", 5, TagCategory.SLEEP);
		var batchB = saveTags("mB", 5, TagCategory.OTHER);
		var docA = userRepository.save(new User("doc-mA-5", true));
		var docB = userRepository.save(new User("doc-mB-5", true));
		var patient = userService.create(createBase("p-accum", false));

		userService.update(patient.getId(), write("p-accum", false, batchA.stream().map(Tag::getId).toList(), docA.getId()));
		userService.update(patient.getId(), write("p-accum", false, batchB.stream().map(Tag::getId).toList(), docB.getId()));

		assertThat(userService.findById(patient.getId()).orElseThrow().getTags()).hasSize(10);
	}

	@Test
	@DisplayName("dois médicos: mesma etiqueta de catálogo ⇒ duas linhas de atribuição; paciente vê etiqueta única")
	void sharedCatalogueTag_twoClinicians_twoAssignments_oneDistinctTag() {
		var shared = tagRepository.save(new Tag("peer-shared-one", null, TagCategory.SLEEP));
		var docFirst = userRepository.save(new User("doc-peer-1st", true));
		var docSecond = userRepository.save(new User("doc-peer-2nd", true));
		var patient = userService.create(createBase("p-peer-share", false));

		userService.update(patient.getId(), write("p-peer-share", false, List.of(shared.getId()), docFirst.getId()));
		userService.update(patient.getId(), write("p-peer-share", false, List.of(shared.getId()), docSecond.getId()));

		User readBack = userService.findById(patient.getId()).orElseThrow();
		assertThat(readBack.getTagAssignments()).hasSize(2);
		assertThat(readBack.getTags()).hasSize(1).extracting(Tag::getId).containsExactly(shared.getId());
	}

	@Test
	@DisplayName("dois médicos com mesma tag: primeiro limpa fatia; segundo mantém ⇒ paciente ainda tem a etiqueta")
	void sharedTag_firstClearsSlice_secondKeepsDistinctUnion() {
		var shared = tagRepository.save(new Tag("peer-rel-gone", null, TagCategory.SLEEP));
		var docFirst = userRepository.save(new User("doc-rel-a", true));
		var docSecond = userRepository.save(new User("doc-rel-b", true));
		var patient = userService.create(createBase("p-peer-rel", false));

		userService.update(patient.getId(), write("p-peer-rel", false, List.of(shared.getId()), docFirst.getId()));
		userService.update(patient.getId(), write("p-peer-rel", false, List.of(shared.getId()), docSecond.getId()));
		userService.update(patient.getId(), write("p-peer-rel", false, List.of(), docFirst.getId()));

		User readBack = userService.findById(patient.getId()).orElseThrow();
		assertThat(readBack.getTagAssignments()).hasSize(1);
		assertThat(readBack.getTags()).hasSize(1).extracting(Tag::getId).containsExactly(shared.getId());
	}

	@Test
	@DisplayName("borda: seis IDs distintos no payload → IllegalArgument mesmo sem passar pela API HTTP")
	void edge_sixDistinctTagIds_throwsIllegalArgument() {
		var tags = saveTags("six", 6, TagCategory.SLEEP);
		var ids = tags.stream().map(Tag::getId).toList();
		var doc = userRepository.save(new User("doc-six-reject", true));
		var patient = userService.create(createBase("p-six-reject", false));
		assertThatThrownBy(() -> userService.update(patient.getId(), write("p-six-reject", false, ids, doc.getId())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("5");
	}

	@Test
	@DisplayName("borda: limpar tags existentes sem assignedByDoctorId → TagAssignmentDoctorRequiredException")
	void edge_clearTagsWithoutDoctor_throws() {
		var tag = tagRepository.save(new Tag("edge-clear-rule", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-clear-edge", true));
		var patient = userService.create(createBase("p-clear-rule", false));
		userService.update(patient.getId(), write("p-clear-rule", false, List.of(tag.getId()), doc.getId()));

		assertThatThrownBy(() -> userService.update(patient.getId(), write("p-clear-rule", false, List.of(), null)))
				.isInstanceOf(TagAssignmentDoctorRequiredException.class);
	}

	@Test
	@DisplayName("borda: mesmo conjunto de tags com ordem diferente no payload → noop sem médico obrigatório")
	void edge_reorderedSameLogicalSet_skipsDoctorCheck() {
		var tWide = tagRepository.save(new Tag("edge-wide-first", null, TagCategory.SLEEP));
		var tNarrow = tagRepository.save(new Tag("edge-narrow-sec", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-order-edge", true));
		var patient = userService.create(createBase("p-order-edge", false));
		userService.update(patient.getId(), write("p-order-edge", false, List.of(tWide.getId(), tNarrow.getId()), doc.getId()));
		userService.update(
				patient.getId(),
				write(
						"p-order-edge",
						false,
						List.of(tNarrow.getId(), tWide.getId()),
						null));
		User readBack = userService.findById(patient.getId()).orElseThrow();
		assertThat(readBack.getTags()).extracting(Tag::getId).containsExactlyInAnyOrder(tWide.getId(), tNarrow.getId());
		assertThat(readBack.getTagAssignments()).hasSize(2);
	}

	@Test
	@DisplayName("borda: UserWriteRequest lista com mais de 5 elementos falha Bean Validation antes do serviço")
	void edge_validator_rejectsSixListElements() {
		var six = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		UserWriteRequest req =
				new UserWriteRequest(
						"bean-six",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						UUID.randomUUID(),
						six);
		Set<ConstraintViolation<UserWriteRequest>> violations = jakartaValidator.validate(req);
		assertThat(violations.stream().filter(v -> "tagIds".equals(v.getPropertyPath().toString())))
				.isNotEmpty();
	}

	@Test
	@DisplayName("borda: getTags lista ids únicos (sem duplicação perceptível via API de leitura)")
	void edge_findById_returnsUniqueTagIdsReadable() {
		var lone = tagRepository.save(new Tag("edge-unique-names", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-uniq-read", true));
		var patient = userService.create(createBase("p-uniq-read", false));
		userService.update(patient.getId(), write("p-uniq-read", false, List.of(lone.getId()), doc.getId()));
		Set<Tag> once = userService.findById(patient.getId()).orElseThrow().getTags();
		assertThat(new HashSet<>(once.stream().map(Tag::getId).toList())).hasSize(once.size());
	}
}
