package com.rb.multi.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * EN: OAuth2-style access token payload (RFC 6749 style field names). PT-BR: Resposta de token estilo OAuth2.
 */
public record TokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("expires_in") long expiresIn) {}
