package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Domain signal that the requested aggregate does not exist (UUID vs {@code code} lookups).</p>
 * <p><b>PT-BR:</b> Sinalização de domínio de que o agregado pedido não existe (consultas por UUID versus {@code code}).</p>
 */
public class UserNotFoundException extends RuntimeException {

	private final UUID userId;
	private final String publicCode;

	private UserNotFoundException(UUID userId, String publicCode, String developerMessage) {
		super(developerMessage);
		this.userId = userId;
		this.publicCode = publicCode;
	}

	/** EN: Missing user by surrogate key. PT-BR: Utilizador inexistente pela chave surrogate. */
	public static UserNotFoundException byId(UUID id) {
		return new UserNotFoundException(id, null, "missing user id " + id);
	}

	/** EN: Missing user by public handle. PT-BR: Utilizador inexistente pelo identificador público. */
	public static UserNotFoundException byCode(String code) {
		return new UserNotFoundException(null, code, "missing user code " + code);
	}

	/**
	 * <p><b>EN:</b> {@code true} when internal UUID keyed the lookup; {@code false} for public {@code code} lookups.</p>
	 * <p><b>PT-BR:</b> {@code true} se o UUID interno foi usado; {@code false} se foi o {@code code} público.</p>
	 */
	public boolean resolvedByUuid() {
		return userId != null;
	}

	/** EN: Template parameters for translators. PT-BR: Parâmetros para traduções de mensagem amigável. */
	public Object[] messageArguments() {
		return resolvedByUuid() ? new Object[] { userId.toString() } : new Object[] { publicCode };
	}
}
