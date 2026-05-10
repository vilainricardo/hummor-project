package com.rb.multi.agent.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.rb.multi.agent.entity.User;

/**
 * EN: Issues access tokens compatible with Spring OAuth2 Resource Server (HS256, {@code iss} / {@code sub}).
 * PT-BR: Emite access tokens compatíveis com o resource server (HS256, {@code iss} / {@code sub}).
 */
@Service
public class JwtTokenService {

	private final byte[] keyBytes;
	private final String issuer;
	private final int expirationSeconds;

	public JwtTokenService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.issuer}") String issuer,
			@Value("${app.jwt.expiration-seconds}") int expirationSeconds) {
		this.keyBytes = requireKey(secret);
		this.issuer = issuer;
		this.expirationSeconds = expirationSeconds;
	}

	private static byte[] requireKey(String secret) {
		byte[] b = secret.getBytes(StandardCharsets.UTF_8);
		if (b.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 UTF-8 bytes for HS256");
		}
		return b;
	}

	public int getExpirationSeconds() {
		return expirationSeconds;
	}

	public String issueAccessToken(User user) {
		Instant now = Instant.now();
		JWTClaimsSet claims =
				new JWTClaimsSet.Builder()
						.issuer(issuer)
						.subject(user.getId().toString())
						.claim("email", user.getEmail())
						.issueTime(Date.from(now))
						.expirationTime(Date.from(now.plusSeconds(expirationSeconds)))
						.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		try {
			jwt.sign(new MACSigner(keyBytes));
			return jwt.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("JWT signing failed", e);
		}
	}
}
