package com.rb.multi.agent.controller;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rb.multi.agent.dto.ApiErrorResponse;
import com.rb.multi.agent.dto.ApiProblemCode;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.rb.multi.agent.controller")
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private final MessageSource messageSource;

	public ApiExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private String msg(String code, Object... args) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(code, args, code, locale);
	}

	private static String path(HttpServletRequest request) {
		return request != null ? request.getRequestURI() : null;
	}

	private ResponseEntity<ApiErrorResponse> respond(
			HttpServletRequest request,
			HttpStatus status,
			ApiProblemCode problemCode,
			String localizedMessage,
			Map<String, String> fieldErrors) {
		ApiErrorResponse body = ApiErrorResponse.of(status, problemCode, localizedMessage, path(request), fieldErrors);
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> userNotFound(UserNotFoundException ex, HttpServletRequest request) {
		ApiProblemCode problem = ex.lookupById() ? ApiProblemCode.USER_NOT_FOUND : ApiProblemCode.USER_NOT_FOUND_BY_CODE;
		String key = ex.lookupById() ? "error.user.notFound" : "error.user.notFoundByCode";
		return respond(request, HttpStatus.NOT_FOUND, problem, msg(key, ex.messageArguments()), null);
	}

	@ExceptionHandler(DuplicateUserCodeException.class)
	public ResponseEntity<ApiErrorResponse> duplicateCode(DuplicateUserCodeException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.CONFLICT,
				ApiProblemCode.USER_CODE_CONFLICT,
				msg("error.user.duplicateCode", ex.getCode()),
				null);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.INVALID_ARGUMENT,
				msg("error.badRequest", ex.getMessage()),
				null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			fields.put(fe.getField(), fe.getDefaultMessage());
		}
		String summary = fields.entrySet().stream()
				.map(e -> e.getKey() + ": " + e.getValue())
				.reduce((a, b) -> a + "; " + b)
				.orElse("");
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.VALIDATION_FAILED,
				msg("error.validation.summary", summary),
				fields);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> internalError(Exception ex, HttpServletRequest request) {
		log.error("Unhandled API error", ex);
		return respond(
				request,
				HttpStatus.INTERNAL_SERVER_ERROR,
				ApiProblemCode.INTERNAL_ERROR,
				msg("error.internal"),
				null);
	}
}
