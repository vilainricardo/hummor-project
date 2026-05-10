package com.rb.multi.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.UserCatalogueTagAssignRequest;
import com.rb.multi.agent.dto.UserCreateRequest;
import com.rb.multi.agent.dto.UserResponse;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.constants.TagCategory;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.repository.MoodEntryRepository;
import com.rb.multi.agent.repository.SleepEntryRepository;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/** EN: {@link UserController} HTTP contract. PT-BR: Contrato HTTP do {@link UserController}. */
class UserControllerIntTest extends AbstractControllerIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MoodEntryRepository moodEntryRepository;

	@Autowired
	private SleepEntryRepository sleepEntryRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static String inboxForCode(String code) {
		return code.trim().toLowerCase(Locale.ROOT) + "@controller.int.test.invalid";
	}

	/** POST: {@link UserCreateRequest} — sem tags (criado sem etiquetas). */
	private static UserCreateRequest userCreatePayload(String code, boolean doctor) {
		return new UserCreateRequest(code, inboxForCode(code), doctor, 30, "x", null, null, null, null, null);
	}

	/** PUT: {@link UserWriteRequest} apenas perfil. */
	private static UserWriteRequest userUpdatePayload(String code, boolean doctor) {
		return new UserWriteRequest(code, inboxForCode(code), doctor, 30, "x", null, null, null, null, null);
	}

	private static UserWriteRequest userUpdatePayload(String code, boolean doctor, String email) {
		return new UserWriteRequest(code, email, doctor, 30, "x", null, null, null, null, null);
	}

	private String tagAssignJson(UUID assignedByDoctorId, UUID tagId) throws Exception {
		return objectMapper.writeValueAsString(new UserCatalogueTagAssignRequest(assignedByDoctorId, tagId));
	}

	private UserResponse readUserResponse(MockHttpServletResponse response) throws Exception {
		return objectMapper.readValue(response.getContentAsString(StandardCharsets.UTF_8), UserResponse.class);
	}

	/** EN: Ensures API JSON never leaks credential fields to the frontend. PT-BR: Garante que o JSON não expõe dados de credencial ao frontend. */
	private void assertJsonTreeHasNoPasswordFields(String jsonBody) {
		try {
			JsonNode root = objectMapper.readTree(jsonBody);
			if (root.isArray()) {
				for (JsonNode elt : root) {
					assertUserJsonObjectHasNoPasswordFields(elt);
				}
				return;
			}
			assertUserJsonObjectHasNoPasswordFields(root);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Invalid JSON in response", e);
		}
	}

	private static void assertUserJsonObjectHasNoPasswordFields(JsonNode node) {
		assertThat(node.isObject())
				.as("Expected JSON object (or array of objects), got: %s", node.getNodeType())
				.isTrue();
		assertThat(node.has("password"))
				.as("Response must not include 'password' (frontend leak)")
				.isFalse();
		assertThat(node.has("passwordHash"))
				.as("Response must not include 'passwordHash' (frontend leak)")
				.isFalse();
	}

	/** POST 201: {@link UserResponse} alinhado a {@link UserCreateRequest}; criação não devolve tags ligadas. */
	private void assertUserPostBodyMatches(UserCreateRequest sent, MvcResult result) throws Exception {
		assertThat(result.getResponse().getStatus()).isEqualTo(201);

		UserResponse body = readUserResponse(result.getResponse());

		String rawId = result.getResponse().getHeader("Location").substring(
				result.getResponse().getHeader("Location").lastIndexOf('/') + 1);

		assertThat(body.id()).isEqualTo(UUID.fromString(rawId));
		assertThat(body.code()).isEqualTo(sent.code().trim());
		assertThat(body.email()).isEqualTo(sent.email().trim().toLowerCase(Locale.ROOT));
		assertThat(body.doctor()).isEqualTo(sent.doctor());
		assertThat(body.age()).isEqualTo(sent.age());
		assertThat(body.profession()).isEqualTo(sent.profession());
		assertThat(body.postalCode()).isEqualTo(sent.postalCode());
		assertThat(body.country()).isEqualTo(sent.country());
		assertThat(body.city()).isEqualTo(sent.city());
		assertThat(body.addressLine()).isEqualTo(sent.addressLine());
		assertThat(body.createdAt()).isNotNull();
		assertThat(body.tags()).isEmpty();
		assertThat(body.selfAssignedTags()).isEmpty();
	}

	@Test
	@DisplayName("GET /api/v1/users — empty")
	void list_whenEmpty_returnsEmptyArray() throws Exception {
		userRepository.deleteAll();
		mockMvc.perform(get("/api/v1/users").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("Credenciais: password e password_hash nunca aparecem nas respostas JSON")
	void responses_neverExposePasswordOrHash_evenWhenPasswordSent() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest create =
				new UserCreateRequest(
						"pw-leak-guard",
						inboxForCode("pw-leak-guard"),
						false,
						30,
						"x",
						null,
						null,
						null,
						null,
						"LeakTest99");
		String createJson = objectMapper.writeValueAsString(create);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), createJson))
						.andExpect(status().isCreated())
						.andReturn();
		assertJsonTreeHasNoPasswordFields(created.getResponse().getContentAsString(StandardCharsets.UTF_8));

		String location = created.getResponse().getHeader("Location");
		UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(get("/api/v1/users/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(
						r ->
								assertJsonTreeHasNoPasswordFields(
										r.getResponse().getContentAsString(StandardCharsets.UTF_8)));

		mockMvc.perform(get("/api/v1/users/by-code/{code}", "pw-leak-guard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(
						r ->
								assertJsonTreeHasNoPasswordFields(
										r.getResponse().getContentAsString(StandardCharsets.UTF_8)));

		mockMvc.perform(get("/api/v1/users").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(
						r ->
								assertJsonTreeHasNoPasswordFields(
										r.getResponse().getContentAsString(StandardCharsets.UTF_8)));

		String putJson =
				objectMapper.writeValueAsString(
						new UserWriteRequest(
								"pw-leak-guard",
								inboxForCode("pw-leak-guard"),
								false,
								30,
								"x",
								null,
								null,
								null,
								null,
								"NewSecret88"));
		MvcResult updated =
				mockMvc.perform(json(put("/api/v1/users/{id}", id), putJson)).andExpect(status().isOk()).andReturn();
		assertJsonTreeHasNoPasswordFields(updated.getResponse().getContentAsString(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("POST /api/v1/users — 201 and Location header")
	void create_returnsCreated() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest sent = userCreatePayload("code-a", false);
		String json = objectMapper.writeValueAsString(sent);
		MvcResult result =
				mockMvc.perform(json(post("/api/v1/users"), json))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(sent, result);
	}

	@Test
	@DisplayName("GET /api/v1/users/{id} and GET /api/v1/users/by-code/{code}")
	void getById_andByCode_roundTrip() throws Exception {
		userRepository.deleteAll();

		UserCreateRequest roundTripSent = userCreatePayload("round-trip", true);
		String payload = objectMapper.writeValueAsString(roundTripSent);

		MvcResult created = mockMvc.perform(json(post("/api/v1/users"), payload))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andReturn();
		assertUserPostBodyMatches(roundTripSent, created);

		String location = created.getResponse().getHeader("Location");
		String rawId = location.substring(location.lastIndexOf('/') + 1);
		UUID id = UUID.fromString(rawId);

		mockMvc.perform(get("/api/v1/users/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));

		mockMvc.perform(get("/api/v1/users/by-code/{code}", "round-trip").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("round-trip"));
	}

	@Test
	@DisplayName("GET /api/v1/users/{id} — not found")
	void getById_missing_returns404() throws Exception {
		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("GET /api/v1/users/by-code/{code} — not found")
	void getByCode_missing_returns404() throws Exception {
		mockMvc.perform(get("/api/v1/users/by-code/{code}", "no-such-public-code").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_CODE"));
	}

	@Test
	@DisplayName("POST /api/v1/users/{id}/tag-assignments — duas tags; lista ordenada alfabeticamente no GET")
	void assignTags_sortsAlphabeticallyOnRead() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var zebra = tagRepository.save(new com.rb.multi.agent.entity.Tag("zzz-zebra", null, TagCategory.OTHER));
		var alpha = tagRepository.save(new com.rb.multi.agent.entity.Tag("aaa-alpha", null, TagCategory.OTHER));
		User doctor = userRepository.save(User.seedClinician("doc-up-tags"));

		UserCreateRequest upTagsCreate = userCreatePayload("up-tags", false);
		String createdJson = objectMapper.writeValueAsString(upTagsCreate);
		MvcResult createdUser =
				mockMvc.perform(json(post("/api/v1/users"), createdJson))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(upTagsCreate, createdUser);
		String location = createdUser.getResponse().getHeader("Location");
		UUID userId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(
						json(
								post("/api/v1/users/{uid}/tag-assignments", userId),
								tagAssignJson(doctor.getId(), zebra.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(1)))
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(0)));
		mockMvc.perform(
						json(
								post("/api/v1/users/{uid}/tag-assignments", userId),
								tagAssignJson(doctor.getId(), alpha.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctor").value(false))
				.andExpect(jsonPath("$.tags", hasSize(2)))
				.andExpect(jsonPath("$.tags[0].name").value(alpha.getName()))
				.andExpect(jsonPath("$.tags[1].name").value(zebra.getName()))
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(0)));

		mockMvc.perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(2)))
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(0)));
	}

	@Test
	@DisplayName("POST …/tag-assignments — unknown tag id")
	void assign_unknownTag_returns400() throws Exception {
		userRepository.deleteAll();
		User doctor = userRepository.save(User.seedClinician("doc-bad-tags"));
		UserCreateRequest badTagsCreate = userCreatePayload("bad-tags", false);
		String createdJson = objectMapper.writeValueAsString(badTagsCreate);
		MvcResult badCreated =
				mockMvc.perform(json(post("/api/v1/users"), createdJson))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(badTagsCreate, badCreated);
		String location = badCreated.getResponse().getHeader("Location");
		UUID userId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		UUID phantom = UUID.randomUUID();
		mockMvc.perform(json(post("/api/v1/users/{uid}/tag-assignments", userId), tagAssignJson(doctor.getId(), phantom)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_REFERENCES_INVALID"));
	}

	@Test
	@DisplayName("POST … — utente paciente não-médico tenta figurar como assignedByDoctorId → 403")
	void assign_actorNotDoctor_returns403() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var catalogueTag =
				tagRepository.save(new com.rb.multi.agent.entity.Tag("actor-test", null, TagCategory.SLEEP));
		var fakerExtra =
				tagRepository.save(new com.rb.multi.agent.entity.Tag("faker-only-extra", null, TagCategory.SLEEP));
		User realDoctor = userRepository.save(User.seedClinician("doctor-actor-it"));
		User faker = userRepository.save(User.seedPatient("not-doc-assign-it"));
		UserCreateRequest patient = userCreatePayload("patient-actor-it", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid = UUID.fromString(
				created.getResponse().getHeader("Location").substring(
						created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(realDoctor.getId(), catalogueTag.getId())))
				.andExpect(status().isOk());
		mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(faker.getId(), fakerExtra.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ASSIGNING_ACTOR_NOT_DOCTOR"));
	}

	@Test
	@DisplayName("POST … — alvo com isDoctor=true: é paciente+e médico e recebe etiqueta de catálogo")
	void assign_targetWithDoctorFlagStillReceivesCatalogueTag_returns200() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var catalogueTag =
				tagRepository.save(new com.rb.multi.agent.entity.Tag("clin-can-be-patient", null, TagCategory.SLEEP));
		User clinicianAssigner = userRepository.save(User.seedClinician("assigner-cli-it"));
		UserCreateRequest clinicianPatient = userCreatePayload("clin-account-it", true);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(clinicianPatient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID cid = UUID.fromString(
				created.getResponse().getHeader("Location").substring(
						created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								post("/api/v1/users/{cid}/tag-assignments", cid),
								tagAssignJson(clinicianAssigner.getId(), catalogueTag.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctor").value(true))
				.andExpect(jsonPath("$.tags", hasSize(1)));
	}

	@Test
	@DisplayName("POST … — sexta etiqueta distinta pelo mesmo médico → TAG_ASSIGNMENT_SLICE_FULL")
	void assign_sixthDistinctTag_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(User.seedClinician("doc-slice-cap"));
		List<UUID> fiveIds =
				IntStream.range(0, 5)
						.mapToObj(i -> tagRepository.save(new com.rb.multi.agent.entity.Tag("cap-" + i, null, TagCategory.SLEEP)))
						.map(com.rb.multi.agent.entity.Tag::getId)
						.toList();
		UUID sixth = tagRepository.save(new com.rb.multi.agent.entity.Tag("cap-six", null, TagCategory.OTHER)).getId();
		MvcResult created =
				mockMvc.perform(
								json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-sixth", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		for (UUID tid : fiveIds) {
			mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctor.getId(), tid)))
					.andExpect(status().isOk());
		}
		mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctor.getId(), sixth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_ASSIGNMENT_SLICE_FULL"));
	}

	@Test
	@DisplayName("POST … — cinco etiquetas válidas pelo mesmo médico → GET com tam. 5")
	void assign_edge_fiveDistinctTags_returns200AndFiveTagsInBody() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(User.seedClinician("doc-bv-five"));
		List<UUID> fiveIds =
				IntStream.range(0, 5)
						.mapToObj(i -> tagRepository.save(new com.rb.multi.agent.entity.Tag("five-" + i, null, TagCategory.OTHER)))
						.map(com.rb.multi.agent.entity.Tag::getId)
						.toList();
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-five-tags", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		for (UUID tid : fiveIds) {
			mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctor.getId(), tid)))
					.andExpect(status().isOk());
		}
		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(5)));
	}

	@Test
	@DisplayName("POST … — dois médicos com 5 ids distintos cada ⇒ GET acumula 10 etiquetas únicas")
	void assign_agg_twoCliniciansEachFiveDistinct_tagsLengthTenOnGet() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctorA = userRepository.save(User.seedClinician("doc-acc-a"));
		User doctorB = userRepository.save(User.seedClinician("doc-acc-b"));
		List<UUID> idsA =
				IntStream.range(0, 5)
						.mapToObj(i -> tagRepository.save(new com.rb.multi.agent.entity.Tag("acca-" + i, null, TagCategory.SLEEP)))
						.map(com.rb.multi.agent.entity.Tag::getId)
						.toList();
		List<UUID> idsB =
				IntStream.range(0, 5)
						.mapToObj(i -> tagRepository.save(new com.rb.multi.agent.entity.Tag("accb-" + i, null, TagCategory.OTHER)))
						.map(com.rb.multi.agent.entity.Tag::getId)
						.toList();
		MvcResult created =
				mockMvc.perform(
								json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-acc-u", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));

		for (UUID tid : idsA) {
			mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctorA.getId(), tid)))
					.andExpect(status().isOk());
		}
		for (UUID tid : idsB) {
			mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctorB.getId(), tid)))
					.andExpect(status().isOk());
		}

		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(10)));
	}

	@Test
	@DisplayName("POST … — dois médicos atribuem a mesma tag; GET deduplica para uma entrada visível")
	void assign_twoCliniciansSameCatalogueTag_patientSeesOneDistinctTag() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var shared = tagRepository.save(new com.rb.multi.agent.entity.Tag("shared-by-two", null, TagCategory.SLEEP));
		User doctorA = userRepository.save(User.seedClinician("doc-share-a"));
		User doctorB = userRepository.save(User.seedClinician("doc-share-b"));
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-share-u", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));

		mockMvc.perform(
						json(
								post("/api/v1/users/{pid}/tag-assignments", pid),
								tagAssignJson(doctorA.getId(), shared.getId())))
				.andExpect(status().isOk());
		mockMvc.perform(
						json(
								post("/api/v1/users/{pid}/tag-assignments", pid),
								tagAssignJson(doctorB.getId(), shared.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(1)))
				.andExpect(jsonPath("$.tags[0].id").value(shared.getId().toString()));
	}

	@Test
	@DisplayName("DELETE … — médico A remove só a sua atrib.; B mantém a mesma tag")
	void delete_oneClinicianAssignment_secondKeepsSharedTag_patientStillSeesTag() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var shared = tagRepository.save(new com.rb.multi.agent.entity.Tag("slice-shared", null, TagCategory.SLEEP));
		User doctorA = userRepository.save(User.seedClinician("doc-slc-a"));
		User doctorB = userRepository.save(User.seedClinician("doc-slc-b"));
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-slc-u", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));

		mockMvc.perform(
						json(
								post("/api/v1/users/{pid}/tag-assignments", pid),
								tagAssignJson(doctorA.getId(), shared.getId())))
				.andExpect(status().isOk());
		mockMvc.perform(
						json(
								post("/api/v1/users/{pid}/tag-assignments", pid),
								tagAssignJson(doctorB.getId(), shared.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(
						delete("/api/v1/users/{pid}/tag-assignments/{tid}", pid, shared.getId())
								.param("assignedByDoctorId", doctorA.getId().toString())
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(1)))
				.andExpect(jsonPath("$.tags[0].id").value(shared.getId().toString()));
	}

	@Test
	@DisplayName("POST … — assignedByDoctorId inexistente → 404 ASSIGNING_DOCTOR_NOT_FOUND")
	void assign_edge_unknownAssigningDoctor_returns404() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var tag = tagRepository.save(new com.rb.multi.agent.entity.Tag("needs-valid-doc-h", null, TagCategory.SLEEP));
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-unk-doctor", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								post("/api/v1/users/{pid}/tag-assignments", pid),
								tagAssignJson(UUID.randomUUID(), tag.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSIGNING_DOCTOR_NOT_FOUND"));
	}

	@Test
	@DisplayName("DELETE … — atribuição inexistente → PATIENT_TAG_ASSIGNMENT_NOT_FOUND")
	void delete_unknownAssignment_returns404() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(User.seedClinician("doc-del-miss"));
		var tag = tagRepository.save(new com.rb.multi.agent.entity.Tag("miss-del", null, TagCategory.SLEEP));
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("del-miss-u", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						delete("/api/v1/users/{pid}/tag-assignments/{tid}", pid, tag.getId())
								.param("assignedByDoctorId", doctor.getId().toString())
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PATIENT_TAG_ASSIGNMENT_NOT_FOUND"));
	}

	@Test
	@DisplayName("PUT /api/v1/users/{id} — perfil sem tocar nas tags já atribuídas")
	void update_profileLeavesTagsUntouched() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var tagB = tagRepository.save(new com.rb.multi.agent.entity.Tag("reorder-beta-h", null, TagCategory.OTHER));
		var tagA = tagRepository.save(new com.rb.multi.agent.entity.Tag("reorder-alpha-h", null, TagCategory.OTHER));
		User doctor = userRepository.save(User.seedClinician("doc-reorder-h"));
		UserCreateRequest patient = userCreatePayload("bd-reorder-ts", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String location = created.getResponse().getHeader("Location");
		UUID pid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctor.getId(), tagB.getId())))
				.andExpect(status().isOk());
		mockMvc.perform(json(post("/api/v1/users/{pid}/tag-assignments", pid), tagAssignJson(doctor.getId(), tagA.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										new UserWriteRequest(
												"bd-rt-ren",
												inboxForCode("bd-reorder-ts"),
												false,
												30,
												"x",
												null,
												null,
												null,
												null,
												null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(2)))
				.andExpect(jsonPath("$.code").value("bd-rt-ren"));

		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.tags", hasSize(2)));
	}

	@Test
	@DisplayName("POST /api/v1/users — duplicate public code conflict")
	void create_duplicateCode_returns409() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest dup = userCreatePayload("dup-x", false);
		MvcResult first =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(dup)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(dup, first);
		mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("dup-x", false))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}

	@Test
	@DisplayName("DELETE /api/v1/users/{id} — 204")
	void delete_existing() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest delSent = userCreatePayload("to-del", false);
		String payload = objectMapper.writeValueAsString(delSent);
		MvcResult createdDel =
				mockMvc.perform(json(post("/api/v1/users"), payload))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(delSent, createdDel);
		String location = createdDel.getResponse().getHeader("Location");
		UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(delete("/api/v1/users/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/users/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("DELETE /api/v1/users/{id} — not found")
	void delete_missing_returns404() throws Exception {
		mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /api/v1/users — Bean Validation (blank code)")
	void create_invalid_returns400Validation() throws Exception {
		String json =
				objectMapper.writeValueAsString(
						new UserCreateRequest("", "valid-skeleton@int.test", false, null, null, null, null, null, null, null));
		mockMvc.perform(json(post("/api/v1/users"), json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.code").exists());
	}

	@Test
	@DisplayName("PUT /api/v1/users/{id} — code conflict against another existing user")
	void update_codeConflict_returns409() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest ownerOne = userCreatePayload("owner-one", false);
		MvcResult createdOne =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(ownerOne)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(ownerOne, createdOne);

		UserCreateRequest ownerTwo = userCreatePayload("owner-two", false);
		MvcResult createdTwo =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(ownerTwo)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(ownerTwo, createdTwo);
		String loc = createdTwo.getResponse().getHeader("Location");
		UUID idTwo = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String conflict =
				objectMapper.writeValueAsString(
						userUpdatePayload("owner-one", false, inboxForCode("owner-one")));
		mockMvc.perform(json(put("/api/v1/users/{id}", idTwo), conflict))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}

	@Test
	@DisplayName("POST /api/v1/users — duplicate email (normalized) conflict")
	void create_duplicateEmail_returns409() throws Exception {
		userRepository.deleteAll();
		String inbox = "shared-overlap@c-it.example.com";
		UserCreateRequest first = new UserCreateRequest("dup-mail-a", inbox, false, 30, "x", null, null, null, null, null);
		mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(first)))
				.andExpect(status().isCreated());
		UserCreateRequest secondSameEmail =
				new UserCreateRequest("dup-mail-b", "Shared-Overlap@C-IT.EXAMPLE.COM", false, 30, "x", null, null, null, null, null);
		mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(secondSameEmail)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_CONFLICT"));
	}

	@Test
	@DisplayName("PUT /api/v1/users/{id} — email conflict against another existing user")
	void update_emailConflict_returns409() throws Exception {
		userRepository.deleteAll();
		UserCreateRequest first = userCreatePayload("keep-code-a", false);
		MvcResult c1 =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(first)))
						.andExpect(status().isCreated())
						.andReturn();
		assertUserPostBodyMatches(first, c1);

		UserCreateRequest second = userCreatePayload("keep-code-b", false);
		MvcResult c2 =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(second)))
						.andExpect(status().isCreated())
						.andReturn();
		assertUserPostBodyMatches(second, c2);

		String loc = c2.getResponse().getHeader("Location");
		UUID idB = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));
		UserWriteRequest stealEmail =
				new UserWriteRequest("keep-code-b", inboxForCode("keep-code-a"), false, 30, "x", null, null, null, null, null);

		mockMvc.perform(json(put("/api/v1/users/{id}", idB), objectMapper.writeValueAsString(stealEmail)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_CONFLICT"));
	}

	@Test
	@DisplayName("POST /api/v1/users/{id}/mood-entries — 201 + segundo no último minuto → 429")
	void createMoodEntry_successThenTooSoon() throws Exception {
		moodEntryRepository.deleteAll();
		userRepository.deleteAll();

		UserCreateRequest patient = userCreatePayload("mood-patient", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String loc = created.getResponse().getHeader("Location");
		UUID patientId = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String body = "{\"value\":7}";
		mockMvc.perform(json(post("/api/v1/users/{id}/mood-entries", patientId), body))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.kind").value("MOOD"))
				.andExpect(jsonPath("$.value").value(7));

		mockMvc.perform(json(post("/api/v1/users/{id}/mood-entries", patientId), body))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("MOOD_ENTRY_TOO_SOON"));
	}

	@Test
	@DisplayName("POST /api/v1/users/{id}/mood-entries — utilizador inexistente → 404")
	void createMoodEntry_unknownUser_returns404() throws Exception {
		moodEntryRepository.deleteAll();
		userRepository.deleteAll();

		UUID random = UUID.randomUUID();
		mockMvc.perform(json(post("/api/v1/users/{id}/mood-entries", random), "{\"value\":5}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /api/v1/users/{id}/mood-entries — valor fora da escala → 400")
	void createMoodEntry_invalidValue_returns400() throws Exception {
		moodEntryRepository.deleteAll();
		userRepository.deleteAll();

		UserCreateRequest patient = userCreatePayload("mood-invalid", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String loc = created.getResponse().getHeader("Location");
		UUID patientId = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		mockMvc.perform(json(post("/api/v1/users/{id}/mood-entries", patientId), "{\"value\":11}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("POST …/sleep-entries/today — segundo no mesmo dia UTC → 409")
	void sleepToday_duplicateDay_returns409() throws Exception {
		moodEntryRepository.deleteAll();
		sleepEntryRepository.deleteAll();
		userRepository.deleteAll();

		UserCreateRequest patient = userCreatePayload("sleep-today", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String loc = created.getResponse().getHeader("Location");
		UUID patientId = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String body = "{\"value\":5}";
		mockMvc.perform(json(post("/api/v1/users/{id}/sleep-entries/today", patientId), body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.kind").value("SLEEP"));

		mockMvc.perform(json(post("/api/v1/users/{id}/sleep-entries/today", patientId), body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SLEEP_ENTRY_DAY_CONFLICT"));
	}

	@Test
	@DisplayName("POST …/sleep-entries/for-date — mesmo dia duas vezes → 409")
	void sleepForDate_duplicate_returns409() throws Exception {
		moodEntryRepository.deleteAll();
		sleepEntryRepository.deleteAll();
		userRepository.deleteAll();

		UserCreateRequest patient = userCreatePayload("sleep-dated", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String loc = created.getResponse().getHeader("Location");
		UUID patientId = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String json = "{\"value\":6,\"date\":\"2024-11-15\"}";
		mockMvc.perform(json(post("/api/v1/users/{id}/sleep-entries/for-date", patientId), json))
				.andExpect(status().isCreated());
		mockMvc.perform(json(post("/api/v1/users/{id}/sleep-entries/for-date", patientId), json))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SLEEP_ENTRY_DAY_CONFLICT"));
	}

	@Test
	@DisplayName("POST …/sleep-entries/for-date — data futura → 400")
	void sleepForDate_futureDate_returns400() throws Exception {
		moodEntryRepository.deleteAll();
		sleepEntryRepository.deleteAll();
		userRepository.deleteAll();

		UserCreateRequest patient = userCreatePayload("sleep-future", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String loc = created.getResponse().getHeader("Location");
		UUID patientId = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String json = "{\"value\":5,\"date\":\"2099-12-31\"}";
		mockMvc.perform(json(post("/api/v1/users/{id}/sleep-entries/for-date", patientId), json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("POST …/self-tag-assignments — paciente liga tag do catálogo; listada em selfAssignedTags")
	void selfAssign_patient_addsTag() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		Tag catalogue = tagRepository.save(new Tag("self-pick-one", null, TagCategory.OTHER));
		UserCreateRequest patient = userCreatePayload("self-tag-p", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID userId =
				UUID.fromString(created.getResponse().getHeader("Location").substring(
						created.getResponse().getHeader("Location").lastIndexOf('/') + 1));

		String body = "{\"tagId\":\"" + catalogue.getId() + "\"}";
		mockMvc.perform(json(post("/api/v1/users/{id}/self-tag-assignments", userId), body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(1)))
				.andExpect(jsonPath("$.selfAssignedTags[0].id").value(catalogue.getId().toString()))
				.andExpect(jsonPath("$.tags", hasSize(0)));

		mockMvc.perform(json(post("/api/v1/users/{id}/self-tag-assignments", userId), body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(1)));

		mockMvc.perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.selfAssignedTags", hasSize(1)));
	}

	@Test
	@DisplayName("POST …/self-tag-assignments — tag desconhecida → 400")
	void selfAssign_unknownTag_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		UserCreateRequest patient = userCreatePayload("self-tag-unk", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID userId =
				UUID.fromString(created.getResponse().getHeader("Location").substring(
						created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		UUID phantom = UUID.randomUUID();
		mockMvc.perform(
						json(
								post("/api/v1/users/{id}/self-tag-assignments", userId),
								"{\"tagId\":\"" + phantom + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_REFERENCES_INVALID"));
	}

	@Test
	@DisplayName("POST …/self-tag-assignments — sexta tag distinta → 400 PATIENT_SELF_TAG_SLICE_FULL")
	void selfAssign_sixthDistinct_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		List<Tag> tags =
				IntStream.range(0, 6)
						.mapToObj(i -> tagRepository.save(new Tag("self-max-" + i, null, TagCategory.OTHER)))
						.toList();
		UserCreateRequest patient = userCreatePayload("self-tag-max", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID userId =
				UUID.fromString(created.getResponse().getHeader("Location").substring(
						created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		for (int i = 0; i < 5; i++) {
			String b = "{\"tagId\":\"" + tags.get(i).getId() + "\"}";
			mockMvc.perform(json(post("/api/v1/users/{id}/self-tag-assignments", userId), b))
					.andExpect(status().isOk());
		}
		mockMvc.perform(
						json(
								post("/api/v1/users/{id}/self-tag-assignments", userId),
								"{\"tagId\":\"" + tags.get(5).getId() + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PATIENT_SELF_TAG_SLICE_FULL"));
	}
}
