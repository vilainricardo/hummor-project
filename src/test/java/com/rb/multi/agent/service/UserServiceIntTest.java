package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.rb.multi.agent.exception.AssigningDoctorNotFoundException;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.PatientTagAssignmentNotFoundException;
import com.rb.multi.agent.exception.TagAssignmentSliceFullException;
import com.rb.multi.agent.exception.UnknownTagReferencesException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * EN: {@link UserService} integration tests.
 * PT-BR: Testes de integração do {@link UserService}.
 */
@SpringBootTest
@Transactional
class UserServiceIntTest {

	private static UserCreateRequest createBase(String code, boolean doctor) {
		return new UserCreateRequest(code, doctor, null, null, null, null, null, null);
	}

	private static UserWriteRequest profile(String code, boolean doctor) {
		return new UserWriteRequest(code, doctor, null, null, null, null, null, null);
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
	@DisplayName("Fluxo: clínico atribui e remove etiquetas; leitura expõe apenas subconjunto do catálogo")
	void clinicianAssignsAndRemoves_subsetOnly() {
		var inCatalogUnused = tagRepository.save(new Tag("zzz-unused", null, TagCategory.OTHER));
		var tB = tagRepository.save(new Tag("released-b", null, TagCategory.SLEEP));
		var tA = tagRepository.save(new Tag("released-a", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-pac901", true));

		var savedPatient = userService.create(createBase("pac-901", false));

		userService.assignCatalogueTag(savedPatient.getId(), doc.getId(), tB.getId());
		userService.assignCatalogueTag(savedPatient.getId(), doc.getId(), tA.getId());

		User readViaCode = userService.findByCode("pac-901").orElseThrow();
		assertThat(readViaCode.isDoctor()).isFalse();
		assertThat(readViaCode.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder(tA.getName(), tB.getName());
		assertThat(readViaCode.getTags()).extracting(Tag::getId).doesNotContain(inCatalogUnused.getId());

		userService.removeCatalogueTag(savedPatient.getId(), doc.getId(), tB.getId());
		assertThat(userService.findById(savedPatient.getId()).orElseThrow().getTags())
				.extracting(Tag::getName)
				.containsExactly(tA.getName());
	}

	@Test
	@DisplayName("flag doctor apenas no perfil; tags mantêm-se quando perfil atualiza doctor")
	void doctorFlag_toggleOnProfileKeepsAssignments() {
		var tagged = tagRepository.save(new Tag("role-proof", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-role-proof", true));
		var u = userService.create(createBase("patient-role", false));

		userService.assignCatalogueTag(u.getId(), doc.getId(), tagged.getId());
		userService.update(u.getId(), profile("patient-role", true));

		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).extracting(Tag::getId).containsExactly(tagged.getId());
	}

	@Test
	@DisplayName("remover todas as etiquetas desse médico deixa paciente sem tags desse médico")
	void removeAllTagsForDoctor_clearsSlice() {
		var t = tagRepository.save(new Tag("solo", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-clear", true));

		var u = userService.create(createBase("p-clear", false));
		userService.assignCatalogueTag(u.getId(), doc.getId(), t.getId());
		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).hasSize(1);

		userService.removeCatalogueTag(u.getId(), doc.getId(), t.getId());
		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).isEmpty();
	}

	@Test
	@DisplayName("médico A remove etiqueta só sua; B mantém as suas")
	void removeOneClinicianSlice_preservesPeers() {
		var tagAonly = tagRepository.save(new Tag("solo-a-own", null, TagCategory.SLEEP));
		var tagBonly = tagRepository.save(new Tag("solo-b-own", null, TagCategory.SLEEP));
		var docA = userRepository.save(new User("doc-a-slice", true));
		var docB = userRepository.save(new User("doc-b-slice", true));

		var u = userService.create(createBase("p-slices", false));
		userService.assignCatalogueTag(u.getId(), docA.getId(), tagAonly.getId());
		userService.assignCatalogueTag(u.getId(), docB.getId(), tagBonly.getId());
		assertThat(userService.findById(u.getId()).orElseThrow().getTags()).hasSize(2);

		userService.removeCatalogueTag(u.getId(), docA.getId(), tagAonly.getId());

		User readBack = userService.findById(u.getId()).orElseThrow();
		assertThat(readBack.getTags()).hasSize(1);
		assertThat(readBack.getTags()).extracting(Tag::getId).containsExactly(tagBonly.getId());
	}

	@Test
	@DisplayName("atribuir duas vezes a mesma tag é idempotente")
	void assign_sameTagTwice_idempotent() {
		var tA = tagRepository.save(new Tag("dup-a", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-dup", true));

		var u = userService.create(createBase("p-dup", false));

		userService.assignCatalogueTag(u.getId(), doc.getId(), tA.getId());
		userService.assignCatalogueTag(u.getId(), doc.getId(), tA.getId());

		var tags = userService.findById(u.getId()).orElseThrow().getTags();
		assertThat(tags).hasSize(1);
	}

	@Test
	@DisplayName("assignedByDoctorId resolve paciente sem perfil médico ⇒ AssigningActorNotDoctorException")
	void assign_actorNotDoctor_throws() {
		var tagged = tagRepository.save(new Tag("need-real-doc", null, TagCategory.OTHER));
		var extraForFaker = tagRepository.save(new Tag("faker-cant-act", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-real", true));
		var faker = userRepository.save(new User("fake-doc", false));
		var patient = userService.create(createBase("p-actor", false));
		userService.assignCatalogueTag(patient.getId(), doc.getId(), tagged.getId());

		assertThatThrownBy(() -> userService.assignCatalogueTag(patient.getId(), faker.getId(), extraForFaker.getId()))
				.isInstanceOf(AssigningActorNotDoctorException.class);
	}

	@Test
	@DisplayName("conta com isDoctor=true no alvo: pode receber tag de catálogo (paciente sempre; médico é extra)")
	void assign_targetWithDoctorFlag_succeeds() {
		var tagged = tagRepository.save(new Tag("clin-has-tags-too", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-target", true));
		var clinicianUser = userService.create(createBase("clin-a", true));
		userService.assignCatalogueTag(clinicianUser.getId(), doc.getId(), tagged.getId());
		assertThat(userService.findById(clinicianUser.getId()).orElseThrow().getTags())
				.extracting(Tag::getId)
				.containsExactly(tagged.getId());
	}

	@Test
	@DisplayName("substituição: nova tag adicionada e antiga removida manualmente")
	void assignThenRemoveOld() {
		var tKeep = tagRepository.save(new Tag("keep-me", null, TagCategory.SLEEP));
		var old = tagRepository.save(new Tag("discard-me", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-sub", true));

		var u = userService.create(createBase("p-sub", false));
		userService.assignCatalogueTag(u.getId(), doc.getId(), old.getId());
		userService.assignCatalogueTag(u.getId(), doc.getId(), tKeep.getId());
		userService.removeCatalogueTag(u.getId(), doc.getId(), old.getId());

		assertThat(userService.findById(u.getId()).orElseThrow().getTags())
				.extracting(Tag::getId)
				.containsExactly(tKeep.getId());
	}

	@Test
	@DisplayName("assign com UUID de tag inexistente → UnknownTagReferencesException")
	void assign_unknownTagIds_throws() {
		var doc = userRepository.save(new User("doc-bad-tags", true));
		var u = userService.create(createBase("p-bad-tags", false));
		assertThatThrownBy(() -> userService.assignCatalogueTag(u.getId(), doc.getId(), UUID.randomUUID()))
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
		assertThatThrownBy(() -> userService.update(UUID.randomUUID(), profile("ghost", false)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("update reusando code de terceiros → DuplicateUserCodeException")
	void update_conflictWithOtherPersistedCode_throws() {
		userService.create(createBase("alice", false));
		var bob = userService.create(createBase("bob", false));
		assertThatThrownBy(() -> userService.update(bob.getId(), profile("alice", false)))
				.isInstanceOf(DuplicateUserCodeException.class);
	}

	@Test
	@DisplayName("update de perfil mantém tags existentes")
	void update_profileOnly_preservesTags() {
		var ta = tagRepository.save(new Tag("stable-left", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-stable", true));

		var u = userService.create(createBase("stable-u", false));
		userService.assignCatalogueTag(u.getId(), doc.getId(), ta.getId());
		userService.update(u.getId(), profile("stable-u-renamed", false));

		var tags = userService.findById(u.getId()).orElseThrow().getTags().stream().map(Tag::getId).toList();

		assertThat(tags).containsExactly(ta.getId());
	}

	@Test
	@DisplayName("delete existe remove o agregado; delete id fantasma lança UserNotFoundException")
	void delete_edgeCases() {
		var catalogueRow = tagRepository.save(new Tag("alive", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-alive", true));

		var u = userService.create(createBase("alive-code", false));
		userService.assignCatalogueTag(u.getId(), doc.getId(), catalogueRow.getId());

		userService.deleteById(u.getId());
		assertThat(userRepository.existsById(u.getId())).isFalse();
		assertThat(tagRepository.existsById(catalogueRow.getId())).isTrue();

		assertThatThrownBy(() -> userService.deleteById(u.getId())).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("findAll inclui tags hidratadas")
	void findAll_includesAssociationsWithTags() {
		var t = tagRepository.save(new Tag("bulk", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-bulk", true));

		var u1 = userService.create(createBase("l1", false));
		userService.assignCatalogueTag(u1.getId(), doc.getId(), t.getId());
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
	@DisplayName("borda: assignedByDoctorId inexistente ⇒ AssigningDoctorNotFoundException")
	void edge_assigningDoctorUnknown_throws() {
		var t = tagRepository.save(new Tag("edge-ghost-doc", null, TagCategory.OTHER));
		var doc = userRepository.save(new User("doc-edge-ghost", true));
		var p = userService.create(createBase("p-ghost-assigner", false));
		userService.assignCatalogueTag(p.getId(), doc.getId(), t.getId());
		UUID stranger = UUID.randomUUID();
		assertThatThrownBy(() -> userService.assignCatalogueTag(p.getId(), stranger, t.getId()))
				.isInstanceOfSatisfying(
						AssigningDoctorNotFoundException.class,
						ex -> assertThat(ex.doctorId()).isEqualTo(stranger));
	}

	@Test
	@DisplayName("borda: até cinco etiquetas distintas por médico no paciente")
	void edge_fiveDistinct_perClinicianAccepted() {
		var tags = saveTags("five", 5, TagCategory.OTHER);
		var doc = userRepository.save(new User("doc-five-cap", true));
		var patient = userService.create(createBase("p-five-cap", false));
		for (Tag t : tags) {
			userService.assignCatalogueTag(patient.getId(), doc.getId(), t.getId());
		}
		assertThat(userService.findById(patient.getId()).orElseThrow().getTags()).hasSize(5);
	}

	@Test
	@DisplayName("vários médicos: cada um até 5 distintos ⇒ paciente acumula união até 10")
	void aggregation_twoDoctorsFiveEach_patientShowsTenDistinctTags() {
		var batchA = saveTags("mA", 5, TagCategory.SLEEP);
		var batchB = saveTags("mB", 5, TagCategory.OTHER);
		var docA = userRepository.save(new User("doc-mA-5", true));
		var docB = userRepository.save(new User("doc-mB-5", true));
		var patient = userService.create(createBase("p-accum", false));

		for (Tag t : batchA) {
			userService.assignCatalogueTag(patient.getId(), docA.getId(), t.getId());
		}
		for (Tag t : batchB) {
			userService.assignCatalogueTag(patient.getId(), docB.getId(), t.getId());
		}

		assertThat(userService.findById(patient.getId()).orElseThrow().getTags()).hasSize(10);
	}

	@Test
	@DisplayName("dois médicos: mesma tag ⇒ duas atribuições; paciente vê uma entrada distinta")
	void sharedCatalogueTag_twoAssignments_oneDistinctTagRead() {
		var shared = tagRepository.save(new Tag("peer-shared-one", null, TagCategory.SLEEP));
		var docFirst = userRepository.save(new User("doc-peer-1st", true));
		var docSecond = userRepository.save(new User("doc-peer-2nd", true));
		var patient = userService.create(createBase("p-peer-share", false));

		userService.assignCatalogueTag(patient.getId(), docFirst.getId(), shared.getId());
		userService.assignCatalogueTag(patient.getId(), docSecond.getId(), shared.getId());

		User readBack = userService.findById(patient.getId()).orElseThrow();
		assertThat(readBack.getTagAssignments()).hasSize(2);
		assertThat(readBack.getTags()).hasSize(1).extracting(Tag::getId).containsExactly(shared.getId());
	}

	@Test
	@DisplayName("sexta etiqueta distinta pelo mesmo médico ⇒ TagAssignmentSliceFullException")
	void edge_sixDistinctForSameDoctor_throws() {
		var tags = saveTags("six", 6, TagCategory.SLEEP);
		var doc = userRepository.save(new User("doc-six-reject", true));
		var patient = userService.create(createBase("p-six-reject", false));
		for (int i = 0; i < 5; i++) {
			userService.assignCatalogueTag(patient.getId(), doc.getId(), tags.get(i).getId());
		}
		assertThatThrownBy(() -> userService.assignCatalogueTag(patient.getId(), doc.getId(), tags.get(5).getId()))
				.isInstanceOf(TagAssignmentSliceFullException.class);
	}

	@Test
	@DisplayName("remove inexistente ⇒ PatientTagAssignmentNotFoundException")
	void remove_unknownTriplet_throws() {
		var shared = tagRepository.save(new Tag("gone", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-miss-rem", true));
		var patient = userService.create(createBase("p-miss-rem", false));
		assertThatThrownBy(() -> userService.removeCatalogueTag(patient.getId(), doc.getId(), shared.getId()))
				.isInstanceOf(PatientTagAssignmentNotFoundException.class);
	}

	@Test
	@DisplayName("getTags lista ids únicos na leitura")
	void edge_findById_returnsUniqueTagIdsReadable() {
		var lone = tagRepository.save(new Tag("edge-unique-names", null, TagCategory.SLEEP));
		var doc = userRepository.save(new User("doc-uniq-read", true));
		var patient = userService.create(createBase("p-uniq-read", false));
		userService.assignCatalogueTag(patient.getId(), doc.getId(), lone.getId());
		Set<Tag> once = userService.findById(patient.getId()).orElseThrow().getTags();
		assertThat(new HashSet<>(once.stream().map(Tag::getId).toList())).hasSize(once.size());
	}
}
