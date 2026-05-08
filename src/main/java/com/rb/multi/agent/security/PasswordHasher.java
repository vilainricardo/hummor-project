package com.rb.multi.agent.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * <p><b>EN:</b> Argon2id password hashing for {@code users.password_hash} (Spring Security defaults).</p>
 * <p><b>PT-BR:</b> Hash de palavra-passe Argon2id para {@code users.password_hash} (parâmetros por defeito do Spring Security).</p>
 */
@Component
public class PasswordHasher {

	private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String encodedHash) {
		return encodedHash != null && encoder.matches(rawPassword, encodedHash);
	}
}
