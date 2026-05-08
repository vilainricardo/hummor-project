package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Unique-index violation on {@code users.email} during write paths (normalized form).</p>
 * <p><b>PT-BR:</b> Violação de índice único em {@code users.email} nas escritas (forma normalizada).</p>
 */
public class DuplicateUserEmailException extends RuntimeException {

	private final String email;

	public DuplicateUserEmailException(String normalizedEmail) {
		super("duplicate user email " + normalizedEmail);
		this.email = normalizedEmail;
	}

	/** EN: Duplicate normalized inbox string. PT-BR: E-mail normalizado já em uso. */
	public String getEmail() {
		return email;
	}
}
