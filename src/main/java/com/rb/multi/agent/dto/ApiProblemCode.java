package com.rb.multi.agent.dto;

/**
 * Código estável do cliente/API (invariante ao idioma). Combina-se com mensagem {@link ApiErrorResponse#message()}
 * localizada.
 */
public enum ApiProblemCode {

	USER_NOT_FOUND,
	USER_NOT_FOUND_BY_CODE,
	USER_CODE_CONFLICT,
	VALIDATION_FAILED,
	INVALID_ARGUMENT,
	INTERNAL_ERROR
}
