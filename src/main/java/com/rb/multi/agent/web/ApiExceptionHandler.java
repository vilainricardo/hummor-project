package com.rb.multi.agent.web;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rb.multi.agent.dto.ApiError;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.UserNotFoundException;

@RestControllerAdvice(basePackages = "com.rb.multi.agent.web")
public class ApiExceptionHandler {

	private final MessageSource messageSource;

	public ApiExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private String msg(String code, Object... args) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(code, args, code, locale);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiError> userNotFound(UserNotFoundException ex) {
		String key = ex.lookupById() ? "error.user.notFound" : "error.user.notFoundByCode";
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(msg(key, ex.messageArguments())));
	}

	@ExceptionHandler(DuplicateUserCodeException.class)
	public ResponseEntity<ApiError> duplicateCode(DuplicateUserCodeException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(msg("error.user.duplicateCode", ex.getCode())));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(msg("error.badRequest", ex.getMessage())));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
		Map<String, String> details = new HashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			details.put(fe.getField(), fe.getDefaultMessage());
		}
		String joined = details.entrySet().stream()
				.map(e -> e.getKey() + ": " + e.getValue())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(msg("error.validation.summary", joined)));
	}
}
