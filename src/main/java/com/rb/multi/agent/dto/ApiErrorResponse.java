package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Saída padrão para erros expostos pela API REST.
 *
 * {@code title} usa a razão HTTP canónica; {@code message} segue idioma solicitado (<code>Accept-Language</code>).
 */
@JsonInclude(Include.NON_NULL)
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String title,
		String code,
		String message,
		String path,
		@JsonInclude(Include.NON_EMPTY)
		Map<String, String> fieldErrors
) {

	private static LinkedHashMap<String, String> copyNonEmpty(Map<String, String> fieldErrors) {
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return null;
		}
		return new LinkedHashMap<>(fieldErrors);
	}

	public static ApiErrorResponse of(
			HttpStatus httpStatus,
			ApiProblemCode problemCode,
			String localizedMessage,
			String path,
			Map<String, String> fieldErrors) {
		return new ApiErrorResponse(
				Instant.now(),
				httpStatus.value(),
				httpStatus.getReasonPhrase(),
				problemCode.name(),
				localizedMessage,
				path,
				copyNonEmpty(fieldErrors));
	}
}
