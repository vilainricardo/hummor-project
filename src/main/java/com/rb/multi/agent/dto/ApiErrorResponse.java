package com.rb.multi.agent.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * <p><b>EN:</b> Standard REST error envelope; {@code title} mirrors HTTP reason phrase;
 * {@code message} resolves via {@code Accept-Language} / {@link org.springframework.context.MessageSource}.</p>
 * <p><b>PT-BR:</b> Envelope padrão de erro REST; {@code title} segue a razão HTTP;
 * {@code message} vem de idioma negociado ({@code Accept-Language}) / {@link org.springframework.context.MessageSource}.</p>
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

	/** EN: Omit empty/null field maps from JSON. PT-BR: Omite mapas de campos vazios/nulos do JSON. */
	private static LinkedHashMap<String, String> copyNonEmpty(Map<String, String> fieldErrors) {
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return null;
		}
		return new LinkedHashMap<>(fieldErrors);
	}

	/**
	 * <p><b>EN:</b> Factory assembling HTTP status, problem code and localized narrative.</p>
	 * <p><b>PT-BR:</b> Fábrica que monta estado HTTP, código de problema e mensagem localizada.</p>
	 */
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
