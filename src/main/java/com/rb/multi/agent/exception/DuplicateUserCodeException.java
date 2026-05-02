package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Unique-index violation on externally visible {@code users.code} during write paths.</p>
 * <p><b>PT-BR:</b> Violação de índice único em {@code users.code} visível durante escritas.</p>
 */
public class DuplicateUserCodeException extends RuntimeException {

	private final String code;

	public DuplicateUserCodeException(String code) {
		super("duplicate user code " + code);
		this.code = code;
	}

	/** EN: Duplicate human-facing locator string. PT-BR: Code duplicado visto pelo utilizador. */
	public String getCode() {
		return code;
	}
}
