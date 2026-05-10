package com.rb.multi.agent.config;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.OAuth2TokenValidator;

import javax.crypto.SecretKey;

/**
 * EN: Configures symmetric HS256 JWT validation when the API is not in {@code app.security.disabled} mode.
 * PT-BR: Validação JWT HS256 simétrica quando a segurança está ativa.
 */
@Configuration
@ConditionalOnProperty(name = "app.security.disabled", havingValue = "false", matchIfMissing = true)
public class JwtDecoderConfig {

	@Bean
	NimbusJwtDecoder jwtDecoder(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.issuer}") String issuer) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 UTF-8 bytes for HS256");
		}
		SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
		decoder.setJwtValidator(issuerValidator);
		return decoder;
	}
}
