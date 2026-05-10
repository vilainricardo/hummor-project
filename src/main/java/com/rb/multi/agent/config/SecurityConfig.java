package com.rb.multi.agent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import com.rb.multi.agent.security.RestAccessDeniedHandler;
import com.rb.multi.agent.security.RestAuthenticationEntryPoint;

/**
 * EN: Stateless JWT (Bearer) for {@code /api/**} except auth and OpenAPI. PT-BR: JWT sem estado; exceções documentadas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			@Autowired(required = false) JwtDecoder jwtDecoder,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler,
			@Value("${app.security.disabled:false}") boolean securityDisabled) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		if (securityDisabled) {
			http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
			return http.build();
		}

		if (jwtDecoder == null) {
			throw new IllegalStateException("JwtDecoder is required when app.security.disabled=false");
		}

		http.authorizeHttpRequests(
				a ->
						a.requestMatchers(
										"/api/v1/auth/**",
										"/v3/api-docs/**",
										"/swagger-ui/**",
										"/swagger-ui.html",
										"/swagger-ui/index.html")
								.permitAll()
								.requestMatchers("/error")
								.permitAll()
								.anyRequest()
								.authenticated());

		http.exceptionHandling(
				e -> e.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler));

		http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
		return http.build();
	}
}
