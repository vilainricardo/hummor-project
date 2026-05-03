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
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
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

	/** POST: {@link UserCreateRequest} — sem {@code tagIds} (sempre criado sem etiquetas). */
	private static UserCreateRequest userCreatePayload(String code, boolean doctor) {
		return new UserCreateRequest(code, doctor, 30, "x", null, null, null, null);
	}

	/** PUT: {@link UserWriteRequest} com {@code tagIds} e opcionalmente {@code assignedByDoctorId}. */
	private static UserWriteRequest userUpdatePayload(String code, List<UUID> tagIds, boolean doctor, UUID assignedByDoctorId) {
		return new UserWriteRequest(code, doctor, 30, "x", null, null, null, null, assignedByDoctorId, tagIds);
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
		/*
		 * --- Guia rápido (primeira vez em testes neste projeto) ---
		 *
		 * 1) O que é isto? Um "teste automatizado": código que executa a aplicação (ou parte dela) e
		 *    VERIFICA se o resultado foi o esperado. Se algo falhar, o build (mvn test) fica vermelho.
		 *
		 * 2) @Test (JUnit 5): marca este método como cenário de teste. Cada @Test corre isolado.
		 *
		 * 3) "Integração" aqui: não testamos só uma função matemática isolada — levantamos o Spring
		 *    quase completo (serviços, controladores, BD em memória H2). Imitamos pedidos HTTP como
		 *    se fosse Postman, mas dentro do Java, via MockMvc (cliente de teste oficial do Spring).
		 *
		 * 4) MockMvc: permite fazer get("/caminho"), post("/caminho"), etc., e depois afirmar coisas
		 *    como "o status HTTP tem de ser 200" ou "no JSON, o campo code tem de ser X". Essas
		 *    afirmações são os .andExpect(...).
		 *
		 * 5) @Transactional nesta hierarquia de classes: no fim do método de teste as alterações à BD
		 *    são desfeitas (rollback), para o próximo teste começar "limpo" sem interferência.
		 *
		 * 6) O que este método prova especificamente: depois de CRIAR um utilizador por POST,
		 *    podemos ACHÁ-LO pelo id (UUID) e também pelo código público "round-trip" — as duas leituras
		 *    têm de funcionar sobre o mesmo registo criado (ida e volta / "round trip").
		 */

		userRepository.deleteAll();

		/*
		 * userCreatePayload(...) usa {@link UserCreateRequest} (sem tagIds). objectMapper serializa para JSON.
		 * code="round-trip", doctor=true, idade fixa 30, profession fixa "x".
		 */
		UserCreateRequest roundTripSent = userCreatePayload("round-trip", true);
		String payload = objectMapper.writeValueAsString(roundTripSent);

		/*
		 * json(...) (método da classe pai) acrescenta: Content-Type application/json + corpo UTF-8 +
		 * cabeçalho Accept pedindo JSON de volta — para o servidor tratar como API JSON.
		 * post("/api/v1/users") → corresponde ao @PostMapping do UserController.
		 */
		MvcResult created = mockMvc.perform(json(post("/api/v1/users"), payload))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andReturn();
		assertUserPostBodyMatches(roundTripSent, created);

		String location = created.getResponse().getHeader("Location");

		/*
		 * HTTP 201 Created = "criei recurso novo". Convém também um cabeçalho Location com a URL onde
		 * esse recurso passa a viver — aqui esperamos algo que termina com /users/<uuid-do-servidor>.
		 * Extrair o UUID do fim dessa URL para usarmos no GET seguinte (simula um cliente real que só
		 * tinha guardado Location após o create).
		 */
		String rawId = location.substring(location.lastIndexOf('/') + 1);
		UUID id = UUID.fromString(rawId);

		/*
		 * jsonPath("$", ...) navega pelo JSON como se fosse "raiz $.campo".
		 * $.id = campo "id" no topo da resposta. .value(...) compara ao UUID em string porque JSON
		 * representa assim. status().isOk() = HTTP 200.
		 */
		mockMvc.perform(get("/api/v1/users/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));

		/*
		 * Segunda forma de ler o mesmo utilizador: só com o código público "round-trip" na URL.
		 * Também esperamos HTTP 200 e que o campo "code" no JSON coincida — confirma o endpoint
		 * GET /api/v1/users/by-code/{code}.
		 */
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
	@DisplayName("PUT /api/v1/users/{id} — update including tagIds")
	void update_existing_withTags_sortsAlphabetically() throws Exception {
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

		List<UUID> orderZebraFirstAlphaSecond = List.of(zebra.getId(), alpha.getId());
		// Paciente apenas: tags só com isDoctor=false; promover para médico exigiria já ter etiquetas válidas antes.
		String updateJson =
				objectMapper.writeValueAsString(userUpdatePayload("up-tags", orderZebraFirstAlphaSecond, false, doctor.getId()));
		mockMvc.perform(json(put("/api/v1/users/{id}", userId), updateJson))
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
	@DisplayName("PUT /api/v1/users/{id} — unknown tag id")
	void update_unknownTag_returns400() throws Exception {
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

		List<UUID> missing = List.of(UUID.randomUUID());
		String bad = objectMapper.writeValueAsString(userUpdatePayload("bad-tags", missing, false, doctor.getId()));
		mockMvc.perform(json(put("/api/v1/users/{id}", userId), bad))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_REFERENCES_INVALID"));
	}

	@Test
	@DisplayName("PUT /api/v1/users/{id} — alteração de tagIds sem assignedByDoctorId → 400")
	void update_tagsChangeWithoutDoctor_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var catalogueTag = tagRepository.save(new com.rb.multi.agent.entity.Tag("only-with-doc", null, TagCategory.OTHER));
		UserCreateRequest patient = userCreatePayload("needs-med", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		String location = created.getResponse().getHeader("Location");
		UUID uid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", uid),
								objectMapper.writeValueAsString(
										userUpdatePayload("needs-med", List.of(catalogueTag.getId()), false, null))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_ASSIGNMENT_DOCTOR_REQUIRED"));
	}

	@Test
	@DisplayName("PUT … — médico atribuí primeiro; utente paciente não-médico tenta criar sua fatia não vazia → 403")
	void update_actorNotDoctor_returns403() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var catalogueTag =
				tagRepository.save(new com.rb.multi.agent.entity.Tag("actor-test", null, TagCategory.SLEEP));
		var fakerOnlyCatalogueTag =
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
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"patient-actor-it", List.of(catalogueTag.getId()), false, realDoctor.getId()))))
				.andExpect(status().isOk());
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"patient-actor-it",
												List.of(fakerOnlyCatalogueTag.getId()),
												false,
												faker.getId()))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ASSIGNING_ACTOR_NOT_DOCTOR"));
	}

	@Test
	@DisplayName("PUT … — conta com isDoctor não recebe tagIds de paciente → 400")
	void update_doctorAccountTags_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var catalogueTag =
				tagRepository.save(new com.rb.multi.agent.entity.Tag("no-clinician-tags-it", null, TagCategory.SLEEP));
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
								put("/api/v1/users/{id}", cid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"clin-account-it", List.of(catalogueTag.getId()), true, clinicianAssigner.getId()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_ASSIGNMENT_PATIENT_ONLY"));
	}

	@Test
	@DisplayName("PUT … — lista tagIds com 6 elementos (limite Bean Validation = 5) → VALIDATION_FAILED")
	void update_edge_sixSlotsInPayload_returns400Validation() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(new User("doc-bv-six", true));
		UserCreateRequest patient = userCreatePayload("bd-six-slot", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		String location = created.getResponse().getHeader("Location");
		UUID uid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
		List<UUID> sixSlots = Stream.generate(UUID::randomUUID).limit(6).toList();
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", uid),
								objectMapper.writeValueAsString(
										userUpdatePayload("bd-six-slot", sixSlots, false, doctor.getId()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.tagIds").exists());
	}

	@Test
	@DisplayName("PUT … — cinco etiquetas válidas pelo mesmo médico (slice máx.) → 200 e corpo tags com tam. 5")
	void update_edge_fiveDistinctTags_returns200AndFiveTagsInBody() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(new User("doc-bv-five", true));
		List<UUID> fiveIds =
				IntStream.range(0, 5)
						.mapToObj(i -> tagRepository.save(new com.rb.multi.agent.entity.Tag("five-" + i, null, TagCategory.OTHER)))
						.map(com.rb.multi.agent.entity.Tag::getId)
						.toList();
		UserCreateRequest patient = userCreatePayload("bd-five-tags", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload("bd-five-tags", fiveIds, false, doctor.getId()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(5)));
	}

	@Test
	@DisplayName("PUT … — dois médicos com 5 id distintos cada ⇒ GET acumula 10 etiquetas únicas")
	void update_agg_twoCliniciansEachFiveDistinct_tagsLengthTenOnGet() throws Exception {
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

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(userUpdatePayload("bd-acc-u", idsA, false, doctorA.getId()))))
				.andExpect(status().isOk());
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(userUpdatePayload("bd-acc-u", idsB, false, doctorB.getId()))))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/{id}", pid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(10)));
	}

	@Test
	@DisplayName("PUT … — segunda manifestação médica tenta ficar dona de etiqueta já atribuída por outro → 409")
	void update_conflict_tagHeldByOtherClinician_returns409() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var held = tagRepository.save(new com.rb.multi.agent.entity.Tag("held-by-a", null, TagCategory.SLEEP));
		User doctorA = userRepository.save(new User("doc-own-a", true));
		User doctorB = userRepository.save(new User("doc-own-b", true));
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userCreatePayload("bd-hold-u", false))))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-hold-u", List.of(held.getId()), false, doctorA.getId()))))
				.andExpect(status().isOk());

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-hold-u", List.of(held.getId()), false, doctorB.getId()))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TAG_HELD_BY_OTHER_CLINICIAN"));
	}

	@Test
	@DisplayName("PUT … — assignedByDoctorId UUID aleatório inexistente → 404 ASSIGNING_DOCTOR_NOT_FOUND")
	void update_edge_unknownAssigningDoctor_returns404() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		var tag = tagRepository.save(new com.rb.multi.agent.entity.Tag("needs-valid-doc-h", null, TagCategory.SLEEP));
		UserCreateRequest patient = userCreatePayload("bd-unk-doctor", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-unk-doctor", List.of(tag.getId()), false, UUID.randomUUID()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSIGNING_DOCTOR_NOT_FOUND"));
	}

	@Test
	@DisplayName("PUT … — dois ids de tag inexistentes + um válido → TAG_REFERENCES_INVALID")
	void update_edge_partiallyUnknownTags_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(new User("doc-mixed-h", true));
		var ok = tagRepository.save(new com.rb.multi.agent.entity.Tag("mixed-ok-t", null, TagCategory.SLEEP));
		UserCreateRequest patient = userCreatePayload("bd-mix-unknown", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		UUID phantom = UUID.randomUUID();
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-mix-unknown",
												List.of(ok.getId(), phantom),
												false,
												doctor.getId()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_REFERENCES_INVALID"));
	}

	@Test
	@DisplayName("PUT … — segunda escrita apenas reordena os mesmos tagIds→ HTTP 200 sem assignedByDoctorId")
	void update_edge_reorderPayloadSameMembershipSecondPutWithoutDoctor_returns200() throws Exception {
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

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-reorder-ts", List.of(tagB.getId(), tagA.getId()), false, doctor.getId()))))
				.andExpect(status().isOk());

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-reorder-ts", List.of(tagA.getId(), tagB.getId()), false, null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", hasSize(2)));
	}

	@Test
	@DisplayName("PUT … — limpar todas as tags sem médico após já ter etiquetas → TAG_ASSIGNMENT_DOCTOR_REQUIRED")
	void update_edge_clearTagsWithoutDoctorAfterHavingTags_returns400() throws Exception {
		userRepository.deleteAll();
		tagRepository.deleteAll();
		User doctor = userRepository.save(new User("doc-clear-h", true));
		var lone = tagRepository.save(new com.rb.multi.agent.entity.Tag("clear-requires-doc", null, TagCategory.SLEEP));
		UserCreateRequest patient = userCreatePayload("bd-clr-rules", false);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(patient)))
						.andExpect(status().isCreated())
						.andReturn();
		UUID pid =
				UUID.fromString(
						created.getResponse().getHeader("Location").substring(
								created.getResponse().getHeader("Location").lastIndexOf('/') + 1));
		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload(
												"bd-clr-rules", List.of(lone.getId()), false, doctor.getId()))))
				.andExpect(status().isOk());

		mockMvc.perform(
						json(
								put("/api/v1/users/{id}", pid),
								objectMapper.writeValueAsString(
										userUpdatePayload("bd-clr-rules", List.of(), false, null))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_ASSIGNMENT_DOCTOR_REQUIRED"));
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

		String conflict =
				objectMapper.writeValueAsString(userUpdatePayload("owner-one", List.of(), false, null));
		mockMvc.perform(json(put("/api/v1/users/{id}", idTwo), conflict))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}
}
