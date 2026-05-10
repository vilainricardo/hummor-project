package com.rb.multi.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import com.rb.multi.agent.dto.LoginRequest;
import com.rb.multi.agent.dto.TokenResponse;
import com.rb.multi.agent.service.AuthService;

import jakarta.validation.Valid;

/**
 * EN: OAuth2-style password grant facade for local JWTs (resource server validates the returned bearer token).
 * PT-BR: Obtenção de JWT local (o resource server valida o Bearer devolvido).
 */
@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/token")
	public ResponseEntity<TokenResponse> token(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.authenticate(request.email(), request.password()));
	}
}
