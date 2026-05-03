package com.rb.multi.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * <p><b>EN:</b> Registers OpenAPI 3 metadata surfaced by Swagger UI (<code>/swagger-ui.html</code>) and JSON
 * (<code>/v3/api-docs</code>) via springdoc.</p>
 * <p><b>PT-BR:</b> Metadados OpenAPI 3 expostos no Swagger UI e no JSON oficial do springdoc.</p>
 */
@Configuration
public class OpenApiConfig {

	@Value("${spring.application.name:hummor-project}")
	private String applicationName;

	@Bean
	public OpenAPI openAPI() {
		String description =
				"REST API: users catalogue, global tags and categories (MindSignal deployment). Locale via Accept-Language.";
		return new OpenAPI()
				.info(new Info().title(applicationName + " API").description(description).version("0.0.1"));
	}
}
