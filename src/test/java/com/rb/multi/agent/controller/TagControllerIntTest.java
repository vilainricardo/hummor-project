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
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.repository.TagRepository;

/** EN: {@link TagController} HTTP contract. PT-BR: Contrato HTTP do {@link TagController}. */
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
		assertThat(body.name()).isEqualTo(sent.name().strip());
		assertThat(body.description()).isEqualTo(expectedPersistedDescription(sent));
		assertThat(body.category()).isEqualTo(sent.category());
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
		tagRepository.save(new com.rb.multi.agent.entity.Tag("t-anx-a", null, TagCategory.ANXIETY));
		mockMvc.perform(
				get("/api/v1/tags")
						.queryParam("category", TagCategory.ANXIETY.name())
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].category").value(TagCategory.ANXIETY.name()));
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
		TagWriteRequest sent = new TagWriteRequest("panic-feelings", null, TagCategory.ANXIETY);
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
				.andExpect(jsonPath("$.name").value("panic-feelings"));
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
		tagRepository.save(new com.rb.multi.agent.entity.Tag("UnifiedName", "d", TagCategory.OTHER));

		mockMvc.perform(get("/api/v1/tags/by-name/{name}", "unifiedname").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("UnifiedName"));
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
	@DisplayName("PUT /api/v1/tags/{id} — update")
	void update_existing() throws Exception {
		tagRepository.deleteAll();
		var saved = tagRepository.save(new com.rb.multi.agent.entity.Tag("rename-me", null, TagCategory.SLEEP));

		String body = objectMapper.writeValueAsString(new TagWriteRequest("renamed-tag", "x", TagCategory.LOW_MOOD));
		mockMvc.perform(json(put("/api/v1/tags/{id}", saved.getId()), body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("renamed-tag"))
				.andExpect(jsonPath("$.category").value("LOW_MOOD"));
	}

	@Test
	@DisplayName("PUT /api/v1/tags/{id} — unknown id")
	void update_whenMissing_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		String body = objectMapper.writeValueAsString(new TagWriteRequest("n", null, TagCategory.OTHER));
		mockMvc.perform(json(put("/api/v1/tags/{id}", id), body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
	}

	@Test
	@DisplayName("DELETE /api/v1/tags/{id} — idempotent-ish 204")
	void delete_existing() throws Exception {
		tagRepository.deleteAll();
		var saved = tagRepository.save(new com.rb.multi.agent.entity.Tag("to-remove", null, TagCategory.OTHER));

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
	@DisplayName("POST /api/v1/tags — duplicate name conflict")
	void create_duplicateName_returns409() throws Exception {
		tagRepository.deleteAll();
		tagRepository.save(new com.rb.multi.agent.entity.Tag("Taken", null, TagCategory.OTHER));

		String body = objectMapper.writeValueAsString(new TagWriteRequest("taken", null, TagCategory.SLEEP));
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
