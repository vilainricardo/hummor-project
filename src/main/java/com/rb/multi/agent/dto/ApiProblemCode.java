package com.rb.multi.agent.dto;

/**
 * <p><b>EN:</b> Stable machine-facing problem code paired with localized {@link ApiErrorResponse#message()}.</p>
 * <p><b>PT-BR:</b> Código estável para clientes/integração, pareado com {@link ApiErrorResponse#message()} localizada.</p>
 */
public enum ApiProblemCode {

	/** EN: Unknown user by UUID. PT-BR: Utilizador inexistente (UUID). */
	USER_NOT_FOUND,
	/** EN: Unknown user by public code. PT-BR: Utilizador inexistente (code público). */
	USER_NOT_FOUND_BY_CODE,
	/** EN: Duplicate public user code. PT-BR: Code de utilizador duplicado. */
	USER_CODE_CONFLICT,
	/** EN: Unknown tag by id. PT-BR: Etiqueta inexistente (id). */
	TAG_NOT_FOUND,
	/** EN: Unknown tag by name. PT-BR: Etiqueta inexistente (nome). */
	TAG_NOT_FOUND_BY_NAME,
	/** EN: Duplicate tag name. PT-BR: Nome de etiqueta duplicado. */
	TAG_NAME_CONFLICT,
	/** EN: Bean validation failed. PT-BR: Falha de validação Bean Validation. */
	VALIDATION_FAILED,
	/** EN: Illegal argument or bad input. PT-BR: Argumento inválido ou entrada incorreta. */
	INVALID_ARGUMENT,
	/** EN: Unhandled server failure. PT-BR: Falha interna não tratada. */
	INTERNAL_ERROR
}
