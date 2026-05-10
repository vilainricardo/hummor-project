package com.rb.multi.agent.service;

import java.util.Locale;
import java.util.Objects;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.rb.multi.agent.dto.TokenResponse;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.repository.UserRepository;
import com.rb.multi.agent.security.JwtTokenService;
import com.rb.multi.agent.security.PasswordHasher;

/**
 * EN: Password authentication and JWT issuance. PT-BR: Autenticação por palavra-passe e emissão de JWT.
 */
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final JwtTokenService jwtTokenService;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.jwtTokenService = jwtTokenService;
	}

	/**
	 * EN: Returns a bearer access token for valid credentials; {@link BadCredentialsException} otherwise.
	 * PT-BR: Devolve token Bearer se credenciais válidas; caso contrário {@link BadCredentialsException}.
	 */
	public TokenResponse authenticate(String rawEmail, String rawPassword) {
		String email = normalizeEmail(rawEmail);
		User user =
				userRepository
						.findByEmail(email)
						.orElseThrow(() -> new BadCredentialsException("invalid credentials"));
		String hash = user.getPasswordHash();
		if (hash == null || !passwordHasher.matches(rawPassword, hash)) {
			throw new BadCredentialsException("invalid credentials");
		}
		String accessToken = jwtTokenService.issueAccessToken(user);
		return new TokenResponse(accessToken, "Bearer", jwtTokenService.getExpirationSeconds());
	}

	private static String normalizeEmail(String raw) {
		String trimmed = Objects.requireNonNull(raw, "email").trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("email must not be blank");
		}
		if (trimmed.length() > 320) {
			throw new IllegalArgumentException("email must be at most 320 characters");
		}
		return trimmed;
	}
}
