package com.rb.multi.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p><b>EN:</b> Optional HTTP redirect {@code GET /} → Swagger UI path when enabled (useful locally).</p>
 * <p><b>PT-BR:</b> Redirecionamento opcional da raiz {@code GET /} para o Swagger quando ativo (ex.: ambiente local).</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.swagger", name = "redirect-root", havingValue = "true")
public class SwaggerRootRedirectConfig implements WebMvcConfigurer {

	@Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
	private String swaggerUiPath;

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addRedirectViewController("/", swaggerUiPath);
	}
}
