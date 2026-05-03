package com.rb.multi.agent.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p><b>EN:</b> Shared MVC + full-context harness (H2 from {@code src/test/resources/application.properties}).</p>
 * <p><b>PT-BR:</b> Base com MockMvc + contexto completo (H2 configurado nos recursos de teste).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractControllerIntTest {

	@Autowired
	protected MockMvc mockMvc;

	/** EN: JSON requests with UTF-8. PT-BR: Pedidos JSON com UTF-8. */
	protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
		return builder
				.contentType(MediaType.APPLICATION_JSON)
				.characterEncoding(StandardCharsets.UTF_8)
				.accept(MediaType.APPLICATION_JSON)
				.content(body != null ? body : "");
	}
}
