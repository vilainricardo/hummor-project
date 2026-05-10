package com.rb.multi.agent.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.UserCreateRequest;

/**
 * EN: Ensures {@code Accept-Language} drives {@link org.springframework.context.MessageSource} for API errors and Bean
 * Validation (see {@link com.rb.multi.agent.config.I18nConfig}).
 * PT-BR: Garante que {@code Accept-Language} controla mensagens de erro e validação.
 */
class AcceptLanguageIntTest extends AbstractControllerIntTest {

	@Autowired
	private ObjectMapper objectMapper;

	private String invalidUserCreateJson() throws Exception {
		return objectMapper.writeValueAsString(
				new UserCreateRequest(
						"",
						"locale-test@int.test.invalid",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						null));
	}

	// ——— 404 USER_NOT_FOUND (i18n/messages_*) ———

	@Test
	@DisplayName("sem Accept-Language → mensagem en-US (User not found)")
	void noHeader_userNotFound_messageUsEnglish() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/users/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value(containsString("User not found")));
	}

	@Test
	@DisplayName("Accept-Language: pt-BR → mensagem em português (Utilizador não encontrado)")
	void ptBr_userNotFound_portuguese() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(
						get("/api/v1/users/{id}", id)
								.header("Accept-Language", "pt-BR")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value(containsString("Utilizador não encontrado")));
	}

	@Test
	@DisplayName("Accept-Language: pt-PT → mensagem em português")
	void ptPt_userNotFound_portugueseEurope() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(
						get("/api/v1/users/{id}", id)
								.header("Accept-Language", "pt-PT")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString("Utilizador não encontrado")));
	}

	@Test
	@DisplayName("Accept-Language: es-ES → mensagem em castelhano (Espanha)")
	void esEs_userNotFound_spanishSpain() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(
						get("/api/v1/users/{id}", id)
								.header("Accept-Language", "es-ES")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString("Usuario no encontrado")));
	}

	@Test
	@DisplayName("Accept-Language: es-MX → bundle es_MX")
	void esMx_userNotFound_spanishLatAm() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(
						get("/api/v1/users/{id}", id)
								.header("Accept-Language", "es-MX")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString("Usuario no encontrado")));
	}

	@Test
	@DisplayName("Accept-Language: en-GB → inglês (bundle en_GB)")
	void enGb_userNotFound_britishLocale() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(
						get("/api/v1/users/{id}", id)
								.header("Accept-Language", "en-GB")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(containsString("User not found")));
	}

	// ——— Bean Validation (ValidationMessages_*) ———

	@Test
	@DisplayName("validação code em branco — en-US (omit header)")
	void validation_blankCode_messageEnglish() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("public user code")));
	}

	@Test
	@DisplayName("validação code em branco — pt-BR")
	void validation_blankCode_portugueseBrazil() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.header("Accept-Language", "pt-BR")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("utilizador")));
	}

	@Test
	@DisplayName("validação code em branco — pt-PT")
	void validation_blankCode_portuguesePortugal() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.header("Accept-Language", "pt-PT")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("utilizador")));
	}

	@Test
	@DisplayName("validação code em branco — en-US explícito")
	void validation_blankCode_explicitUsEnglish() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.header("Accept-Language", "en-US")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("public user code")));
	}

	@Test
	@DisplayName("validação code em branco — es-MX")
	void validation_blankCode_spanishMexico() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.header("Accept-Language", "es-MX")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("usuario")));
	}

	@Test
	@DisplayName("validação code em branco — es-ES")
	void validation_blankCode_spanishSpain() throws Exception {
		mockMvc.perform(
						json(post("/api/v1/users"), invalidUserCreateJson())
								.header("Accept-Language", "es-ES")
								.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").value(containsString("usuario")));
	}
}
