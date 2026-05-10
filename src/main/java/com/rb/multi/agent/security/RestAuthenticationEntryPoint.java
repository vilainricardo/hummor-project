package com.rb.multi.agent.security;

import java.io.IOException;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rb.multi.agent.dto.ApiErrorResponse;
import com.rb.multi.agent.dto.ApiProblemCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * EN: JSON 401 for unauthenticated API access (JWT missing/invalid). PT-BR: JSON 401 quando o JWT falta ou é inválido.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(MessageSource messageSource, ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		String msg =
				messageSource.getMessage(
						"error.auth.unauthenticated", null, "error.auth.unauthenticated", LocaleContextHolder.getLocale());
		ApiErrorResponse body =
				ApiErrorResponse.of(
						HttpStatus.UNAUTHORIZED, ApiProblemCode.UNAUTHENTICATED, msg, request.getRequestURI(), null);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
