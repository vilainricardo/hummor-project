package com.rb.multi.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * <p><b>EN:</b> Registers OpenAPI 3 metadata surfaced by Swagger UI (<code>/swagger-ui.html</code>) and JSON
 * (<code>/v3/api-docs</code>) via springdoc. Documents the global {@code Accept-Language} header once for all operations
 * (actual negotiation is {@link I18nConfig}—not per-controller).</p>
 * <p><b>PT-BR:</b> Metadados OpenAPI 3; cabeçalho {@code Accept-Language} documentado globalmente (negociação real em
 * {@link I18nConfig}).</p>
 */
@Configuration
public class OpenApiConfig {

	@Value("${spring.application.name:hummor-project}")
	private String applicationName;

	@Bean
	public OpenAPI openAPI() {
		String description =
				"REST API: users catalogue, global tags and categories (MindSignal deployment). "
						+ "Locales use language+region: `en-US`, `pt-BR`, `es-ES`, `es-MX`, etc. "
						+ "Send `Accept-Language`; omit → `en-US`.";
		return new OpenAPI()
				.info(new Info().title(applicationName + " API").description(description).version("0.0.1"))
				.components(
						new Components()
								.addSecuritySchemes(
										"bearer-jwt",
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.description(
														"Access token from POST /api/v1/auth/token (email + password). "
																+ "Send as Authorization: Bearer <token>.")))
				.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}

	/**
	 * EN: Injects a single reusable header description into every operation so clients/Swagger see locale negotiation without
	 * annotating each endpoint. PT-BR: Um cabeçalho documentado em todas as operações sem repetir nos controladores.
	 */
	@Bean
	public OpenApiCustomizer acceptLanguageHeaderCustomizer() {
		return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
			boolean already =
					operation.getParameters() != null
							&& operation.getParameters().stream()
									.anyMatch(p -> p != null && "Accept-Language".equalsIgnoreCase(p.getName()));
			if (already) {
				return;
			}
			operation.addParametersItem(
					new Parameter()
							.in("header")
							.name("Accept-Language")
							.description(
									"Preferred locale for API messages (RFC 5646 / BCP 47), e.g. `en-US`, `pt-BR`, `es-MX`. "
											+ "Omit → `en-US`.")
							.required(false)
							.schema(new StringSchema()._default("en-US").example("pt-BR")));
		}));
	}
}
