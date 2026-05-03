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
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.UserCatalogueTagAssignRequest;
import com.rb.multi.agent.dto.UserCreateRequest;
import com.rb.multi.agent.dto.UserResponse;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/** EN: {@link UserController} HTTP contract. PT-BR: Contrato HTTP do {@link UserController}. */
class UserControllerIntTest extends AbstractControllerIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	/** POST: {@link UserCreateRequest} — sem tags (criado sem etiquetas). */
	private static UserCreateRequest userCreatePayload(String code, boolean doctor) {
		return new UserCreateRequest(code, doctor, 30, "x", null, null, null, null);
	}

	/** PUT: {@link UserWriteRequest} apenas perfil. */
	private static UserWriteRequest userUpdatePayload(String code, boolean doctor) {
		return new UserWriteRequest(code, doctor, 30, "x", null, null, null, null);
	}

	private String tagAssignJson(UUID assignedByDoctorId, UUID tagId) throws Exception {
		return objectMapper.writeValueAsString(new UserCatalogueTagAssignRequest(assignedByDoctorId, tagId));
	}

	private UserResponse readUserResponse(MockHttpServletResponse response) throws Exception {
		return objectMapper.readValue(response.getContentAsString(StandardCharsets.UTF_8), UserResponse.class);
	}

	/** POST 201: {@link UserResponse} alinhado a {@link UserCreateRequest}; criação não devolve tags ligadas. */
	private void assertUserPostBodyMatches(UserCreateRequest sent, MvcResult result) throws Exception {
		assertThat(result.getResponse().getStatus()).isEqualTo(201);

		UserResponse body = readUserResponse(result.getResponse());

		String rawId = result.getResponse().getHeader("Location").substring(
				result.getResponse().getHeader("Location").lastIndexOf('/') + 1);

		assertThat(body.id()).isEqualTo(UUID.fromString(rawId));
		assertThat(body.code()).isEqualTo(sent.code().trim());
		assertThat(body.doctor()).isEqualTo(sent.doctor());
		assertThat(body.age()).isEqualTo(sent.age());
		assertThat(body.profession()).isEqualTo(sent.profession());
		assertThat(body.postalCode()).isEqualTo(sent.postalCode());
		assertThat(body.country()).isEqualTo(sent.country());
		assertThat(body.city()).isEqualTo(sent.city());
		assertThat(body.addressLine()).isEqualTo(sent.addressLine());
		assertThat(body.createdAt()).isNotNull();
		assertThat(body.tags()).isEmpty();
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
		User doctor = userRepository.save(new User("doc-up-tags", true));

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
				.andExpect(jsonPath("$.tags", hasSize(1)));
		mockMvc.perform(
						json(
								post("/api/v1/users/{uid}/tag-assignments", userId),
								tagAssignJson(doctor.getId(), alpha.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctor").value(false))
				.andExpect(jsonPath("$.tags", hasSize(2)))
				.andExpect(jsonPath("$.tags[0].name").value(alpha.getName()))
				.andExpect(jsonPath("$.tags[1].name").value(zebra.getName()));

		mockMvc.perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(2)));
	}

	@Test
	@DisplayName("POST …/tag-assignments — unknown tag id")
	void assign_unknownTag_returns400() throws Exception {
		userRepository.deleteAll();
		User doctor = userRepository.save(new User("doc-bad-tags", true));
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
		User realDoctor = userRepository.save(new User("doctor-actor-it", true));
		User faker = userRepository.save(new User("not-doc-assign-it", false));
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
		User clinicianAssigner = userRepository.save(new User("assigner-cli-it", true));
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
		User doctor = userRepository.save(new User("doc-slice-cap", true));
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
		User doctor = userRepository.save(new User("doc-bv-five", true));
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
		User doctorA = userRepository.save(new User("doc-acc-a", true));
		User doctorB = userRepository.save(new User("doc-acc-b", true));
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
		User doctorA = userRepository.save(new User("doc-share-a", true));
		User doctorB = userRepository.save(new User("doc-share-b", true));
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
		User doctorA = userRepository.save(new User("doc-slc-a", true));
		User doctorB = userRepository.save(new User("doc-slc-b", true));
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
		User doctor = userRepository.save(new User("doc-del-miss", true));
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
		User doctor = userRepository.save(new User("doc-reorder-h", true));
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

		mockMvc.perform(json(put("/api/v1/users/{id}", pid), objectMapper.writeValueAsString(userUpdatePayload("bd-rt-ren", false))))
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
				objectMapper.writeValueAsString(new UserCreateRequest("", false, null, null, null, null, null, null));
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

		String conflict = objectMapper.writeValueAsString(userUpdatePayload("owner-one", false));
		mockMvc.perform(json(put("/api/v1/users/{id}", idTwo), conflict))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}
}
