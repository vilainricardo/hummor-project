package com.rb.multi.agent.security;

import java.io.IOException;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.ApiErrorResponse;
import com.rb.multi.agent.dto.ApiProblemCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * EN: JSON 403 when authentication exists but access is denied. PT-BR: JSON 403 para acesso negado autenticado.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public RestAccessDeniedHandler(MessageSource messageSource, ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(
			HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException {
		String msg =
				messageSource.getMessage(
						"error.auth.accessDenied", null, "error.auth.accessDenied", LocaleContextHolder.getLocale());
		ApiErrorResponse body =
				ApiErrorResponse.of(HttpStatus.FORBIDDEN, ApiProblemCode.ACCESS_DENIED, msg, request.getRequestURI(), null);
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
