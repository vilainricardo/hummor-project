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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.UserResponse;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.TagCategory;
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

	/**
	 * Construtor de corpo HTTP para criar/atualizar utilizadores: fixa perfil arbitrário mas válido
	 * (idade 30, profession "x", demais opcionais null). Parâmetros do teste: {@code code}, {@code doctor},
	 * {@code tagIds}.
	 */
	private static UserWriteRequest userPayload(String code, List<UUID> tagIds, boolean doctor) {
		return new UserWriteRequest(code, doctor, 30, "x", null, null, null, null, tagIds);
	}

	private UserResponse readUserResponse(MockHttpServletResponse response) throws Exception {
		return objectMapper.readValue(response.getContentAsString(StandardCharsets.UTF_8), UserResponse.class);
	}

	/** Garante corpo POST 201 alinhado a {@link UserResponse} vs pedido persistido (+ contagem esperada em tags já resolvida pelo serviço). */
	private void assertUserPostBodyMatches(UserWriteRequest sent, MvcResult result, int expectedTagCount) throws Exception {
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
		assertThat(body.tags()).hasSize(expectedTagCount);
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
		UserWriteRequest sent = userPayload("code-a", List.of(), false);
		String json = objectMapper.writeValueAsString(sent);
		MvcResult result =
				mockMvc.perform(json(post("/api/v1/users"), json))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(sent, result, 0);
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
		 * userPayload(...) constrói um objeto Java UserWriteRequest. objectMapper converte esse objeto em
		 * TEXTO JSON (String) — o mesmo formato que mandarias no Postman no corpo do POST.
		 * Neste exemplo: code="round-trip", doctor=true, tagIds=[], idade fixa 30, profession fixa "x".
		 */
		UserWriteRequest roundTripSent = userPayload("round-trip", List.of(), true);
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
		assertUserPostBodyMatches(roundTripSent, created, 0);

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

		UserWriteRequest upTagsCreate = userPayload("up-tags", List.of(), false);
		String createdJson = objectMapper.writeValueAsString(upTagsCreate);
		MvcResult createdUser =
				mockMvc.perform(json(post("/api/v1/users"), createdJson))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(upTagsCreate, createdUser, 0);
		String location = createdUser.getResponse().getHeader("Location");
		UUID userId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		List<UUID> orderZebraFirstAlphaSecond = List.of(zebra.getId(), alpha.getId());
		String updateJson =
				objectMapper.writeValueAsString(userPayload("up-tags", orderZebraFirstAlphaSecond, true));
		mockMvc.perform(json(put("/api/v1/users/{id}", userId), updateJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctor").value(true))
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
		UserWriteRequest badTagsCreate = userPayload("bad-tags", List.of(), false);
		String createdJson = objectMapper.writeValueAsString(badTagsCreate);
		MvcResult badCreated =
				mockMvc.perform(json(post("/api/v1/users"), createdJson))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(badTagsCreate, badCreated, 0);
		String location = badCreated.getResponse().getHeader("Location");
		UUID userId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		List<UUID> missing = List.of(UUID.randomUUID());
		String bad = objectMapper.writeValueAsString(userPayload("bad-tags", missing, false));
		mockMvc.perform(json(put("/api/v1/users/{id}", userId), bad))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAG_REFERENCES_INVALID"));
	}

	@Test
	@DisplayName("POST /api/v1/users — duplicate public code conflict")
	void create_duplicateCode_returns409() throws Exception {
		userRepository.deleteAll();
		UserWriteRequest dup = userPayload("dup-x", List.of(), false);
		MvcResult first =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(dup)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(dup, first, 0);
		mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(userPayload("dup-x", List.of(), false))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}

	@Test
	@DisplayName("DELETE /api/v1/users/{id} — 204")
	void delete_existing() throws Exception {
		userRepository.deleteAll();
		UserWriteRequest delSent = userPayload("to-del", List.of(), false);
		String payload = objectMapper.writeValueAsString(delSent);
		MvcResult createdDel =
				mockMvc.perform(json(post("/api/v1/users"), payload))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(delSent, createdDel, 0);
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
				objectMapper.writeValueAsString(new UserWriteRequest("", false, null, null, null, null, null, null, List.of()));
		mockMvc.perform(json(post("/api/v1/users"), json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.code").exists());
	}

	@Test
	@DisplayName("PUT /api/v1/users/{id} — code conflict against another existing user")
	void update_codeConflict_returns409() throws Exception {
		userRepository.deleteAll();
		UserWriteRequest ownerOne = userPayload("owner-one", List.of(), false);
		MvcResult createdOne =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(ownerOne)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(ownerOne, createdOne, 0);

		UserWriteRequest ownerTwo = userPayload("owner-two", List.of(), false);
		MvcResult createdTwo =
				mockMvc.perform(json(post("/api/v1/users"), objectMapper.writeValueAsString(ownerTwo)))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertUserPostBodyMatches(ownerTwo, createdTwo, 0);
		String loc = createdTwo.getResponse().getHeader("Location");
		UUID idTwo = UUID.fromString(loc.substring(loc.lastIndexOf('/') + 1));

		String conflict =
				objectMapper.writeValueAsString(userPayload("owner-one", List.of(), false));
		mockMvc.perform(json(put("/api/v1/users/{id}", idTwo), conflict))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_CODE_CONFLICT"));
	}
}
