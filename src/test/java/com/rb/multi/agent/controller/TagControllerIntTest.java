package com.rb.multi.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
import com.rb.multi.agent.dto.TagResponse;
import com.rb.multi.agent.dto.TagWriteRequest;
import com.rb.multi.agent.constants.TagCategory;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.support.CatalogueTags;

/** EN: {@link com.rb.multi.agent.controller.TagController} HTTP contract. PT-BR: Contrato HTTP das tags. */
class TagControllerIntTest extends AbstractControllerIntTest {

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private TagResponse readTagResponse(MockHttpServletResponse response) throws Exception {
		return objectMapper.readValue(response.getContentAsString(StandardCharsets.UTF_8), TagResponse.class);
	}

	/** Mesma convenção da camada de serviço para descrição persistida ({@code blank} → {@code null}). */
	private static String expectedPersistedDescription(TagWriteRequest sent) {
		String d = sent.description();
		if (d == null) {
			return null;
		}
		String t = d.strip();
		return t.isEmpty() ? null : t;
	}

	private void assertTagPostBodyMatches(TagWriteRequest sent, MvcResult result) throws Exception {
		assertThat(result.getResponse().getStatus()).isEqualTo(201);
		String loc = result.getResponse().getHeader("Location");
		assertThat(loc).isNotNull();
		String rawId = loc.substring(loc.lastIndexOf('/') + 1);

		TagResponse body = readTagResponse(result.getResponse());

		assertThat(body.id()).isEqualTo(UUID.fromString(rawId));
		assertThat(body.code()).isEqualTo(Tag.normalizeCatalogCode(sent.code()));
		assertThat(body.name()).isEqualTo(sent.name().strip());
		assertThat(body.description()).isEqualTo(expectedPersistedDescription(sent));
		assertThat(body.categories()).containsExactlyInAnyOrderElementsOf(sent.categories());
		assertThat(body.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("GET /api/v1/tags — empty catalogue")
	void list_whenEmpty_returnsEmptyJsonArray() throws Exception {
		tagRepository.deleteAll();
		mockMvc.perform(get("/api/v1/tags").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("GET /api/v1/tags — filter by category")
	void list_whenCategory_returnsSlice() throws Exception {
		tagRepository.deleteAll();
		tagRepository.save(CatalogueTags.seed("t-anx-a", null, TagCategory.ANXIETY));
		mockMvc.perform(
				get("/api/v1/tags")
						.queryParam("category", TagCategory.ANXIETY.name())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].categories", hasItem("ANXIETY")))
				.andExpect(jsonPath("$[0].code").value("T_ANX_A"));
	}

	@Test
	@DisplayName("GET /api/v1/tags?category=invalid — 400")
	void list_whenInvalidEnum_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/tags").queryParam("category", "NOT_A_CATEGORY").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
	}

	@Test
	@DisplayName("POST + GET /api/v1/tags/{id} — happy path")
	void create_thenGetById() throws Exception {
		tagRepository.deleteAll();
		TagWriteRequest sent =
				new TagWriteRequest("PANIC_FEELINGS", "panic-feelings", null, List.of(TagCategory.ANXIETY));
		String body = objectMapper.writeValueAsString(sent);
		MvcResult created =
				mockMvc.perform(json(post("/api/v1/tags"), body))
						.andExpect(status().isCreated())
						.andExpect(header().exists("Location"))
						.andReturn();
		assertTagPostBodyMatches(sent, created);
		String location = created.getResponse().getHeader("Location");

		String rawId = location.substring(location.lastIndexOf('/') + 1);
		UUID id = UUID.fromString(rawId);

		mockMvc.perform(get("/api/v1/tags/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.code").value("PANIC_FEELINGS"))
				.andExpect(jsonPath("$.name").value("panic-feelings"))
				.andExpect(jsonPath("$.categories", hasItem("ANXIETY")));
	}

	@Test
	@DisplayName("GET /api/v1/tags/{id} — not found")
	void getById_whenMissing_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/tags/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("GET /api/v1/tags/by-name/{name} — resolves case-insensitively")
	void getByName_found() throws Exception {
		tagRepository.deleteAll();
		tagRepository.save(CatalogueTags.seed("UnifiedName", "d", TagCategory.OTHER));

		mockMvc.perform(get("/api/v1/tags/by-name/{name}", "unifiedname").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("UNIFIEDNAME"))
				.andExpect(jsonPath("$.name").value("UnifiedName"))
				.andExpect(jsonPath("$.categories", hasItem("OTHER")));
	}

	@Test
	@DisplayName("GET /api/v1/tags/by-name/{name} — not found")
	void getByName_whenMissing_returns404() throws Exception {
		tagRepository.deleteAll();
		mockMvc.perform(get("/api/v1/tags/by-name/{name}", "ghost").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND_BY_NAME"));
	}

	@Test
	@DisplayName("GET /api/v1/tags/not-a-uuid — type mismatch")
	void getById_invalidUuid_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/tags/not-a-uuid").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
	}

	@Test
	@DisplayName("PUT /api/v1/tags/{id} — body code must match persisted code")
	void update_wrongCode_returns400() throws Exception {
		tagRepository.deleteAll();
		var saved = tagRepository.save(CatalogueTags.seed("stay", null, TagCategory.OTHER));
		String body =
				objectMapper.writeValueAsString(new TagWriteRequest("WRONG", "stay", null, List.of(TagCategory.OTHER)));
		mockMvc.perform(json(put("/api/v1/tags/{id}", saved.getId()), body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
	}

	@Test
	@DisplayName("PUT /api/v1/tags/{id} — update")
	void update_existing() throws Exception {
		tagRepository.deleteAll();
		var saved = tagRepository.save(CatalogueTags.seed("rename-me", null, TagCategory.SLEEP));

		String body =
				objectMapper.writeValueAsString(
						new TagWriteRequest(
								"RENAME_ME", "renamed-tag", "x", List.of(TagCategory.LOW_MOOD, TagCategory.SLEEP)));
		mockMvc.perform(json(put("/api/v1/tags/{id}", saved.getId()), body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("RENAME_ME"))
				.andExpect(jsonPath("$.name").value("renamed-tag"))
				.andExpect(jsonPath("$.categories", hasItem("LOW_MOOD")))
				.andExpect(jsonPath("$.categories", hasItem("SLEEP")));
	}

	@Test
	@DisplayName("PUT /api/v1/tags/{id} — unknown id")
	void update_whenMissing_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		String body = objectMapper.writeValueAsString(new TagWriteRequest("N", "n", null, List.of(TagCategory.OTHER)));
		mockMvc.perform(json(put("/api/v1/tags/{id}", id), body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("DELETE /api/v1/tags/{id} — idempotent-ish 204")
	void delete_existing() throws Exception {
		tagRepository.deleteAll();
		var saved = tagRepository.save(CatalogueTags.seed("to-remove", null, TagCategory.OTHER));

		mockMvc.perform(delete("/api/v1/tags/{id}", saved.getId()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/tags/{id}", saved.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("DELETE /api/v1/tags/{id} — not found")
	void delete_whenMissing_returns404() throws Exception {
		mockMvc.perform(delete("/api/v1/tags/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("POST /api/v1/tags — duplicate code conflict")
	void create_duplicateCode_returns409() throws Exception {
		tagRepository.deleteAll();
		tagRepository.save(CatalogueTags.seed("first-label", null, TagCategory.SLEEP));
		String body =
				objectMapper.writeValueAsString(
						new TagWriteRequest("FIRST_LABEL", "second-label", null, List.of(TagCategory.ANXIETY)));
		mockMvc.perform(json(post("/api/v1/tags"), body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TAG_CODE_CONFLICT"));
	}

	@Test
	@DisplayName("POST /api/v1/tags — duplicate name conflict")
	void create_duplicateName_returns409() throws Exception {
		tagRepository.deleteAll();
		tagRepository.save(CatalogueTags.seed("Taken", null, TagCategory.OTHER));

		String body =
				objectMapper.writeValueAsString(
						new TagWriteRequest("OTHER", "taken", null, List.of(TagCategory.SLEEP)));
		mockMvc.perform(json(post("/api/v1/tags"), body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TAG_NAME_CONFLICT"));
	}

	@Test
	@DisplayName("POST /api/v1/tags — Bean Validation failures")
	void create_invalid_returns400WithValidationCode() throws Exception {
		mockMvc.perform(json(post("/api/v1/tags"), "{}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors").exists());
	}
}
