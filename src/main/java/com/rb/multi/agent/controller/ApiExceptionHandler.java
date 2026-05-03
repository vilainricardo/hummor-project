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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.rb.multi.agent.dto.ApiErrorResponse;
import com.rb.multi.agent.dto.ApiProblemCode;
import com.rb.multi.agent.exception.AssigningActorNotDoctorException;
import com.rb.multi.agent.exception.AssigningDoctorNotFoundException;
import com.rb.multi.agent.exception.DuplicateTagNameException;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.TagAssignmentDoctorRequiredException;
import com.rb.multi.agent.exception.TagAssignmentPatientOnlyException;
import com.rb.multi.agent.exception.TagHeldByOtherClinicianException;
import com.rb.multi.agent.exception.TagNotFoundException;
import com.rb.multi.agent.exception.UnknownTagReferencesException;
import com.rb.multi.agent.exception.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p><b>EN:</b> Maps controller-layer exceptions into {@link ApiErrorResponse}; messages from {@link MessageSource}.</p>
 * <p><b>PT-BR:</b> Mapeia exceções da camada de controladores para {@link ApiErrorResponse}; mensagens via
 * {@link MessageSource}.</p>
 */
@RestControllerAdvice(basePackages = "com.rb.multi.agent.controller")
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private final MessageSource messageSource;

	public ApiExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/** EN: Resolve i18n key with current locale. PT-BR: Resolve chave i18n com o locale atual. */
	private String msg(String code, Object... args) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(code, args, code, locale);
	}

	/** EN: Request path for diagnostics. PT-BR: Caminho HTTP para diagnóstico. */
	private static String path(HttpServletRequest request) {
		return request != null ? request.getRequestURI() : null;
	}

	/**
	 * <p><b>EN:</b> Builds Problem+HTTP response with optional field-level validation map.</p>
	 * <p><b>PT-BR:</b> Constrói resposta problema+HTTP com mapa opcional de campos validados.</p>
	 */
	private ResponseEntity<ApiErrorResponse> respond(
			HttpServletRequest request,
			HttpStatus status,
			ApiProblemCode problemCode,
			String localizedMessage,
			Map<String, String> fieldErrors) {
		ApiErrorResponse body = ApiErrorResponse.of(status, problemCode, localizedMessage, path(request), fieldErrors);
		return ResponseEntity.status(status).body(body);
	}

	/** EN: User missing by UUID or public code. PT-BR: Utilizador inexistente por UUID ou code público. */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> userNotFound(UserNotFoundException ex, HttpServletRequest request) {
		ApiProblemCode problem = ex.resolvedByUuid() ? ApiProblemCode.USER_NOT_FOUND : ApiProblemCode.USER_NOT_FOUND_BY_CODE;
		String key = ex.resolvedByUuid() ? "error.user.notFound" : "error.user.notFoundByCode";
		return respond(request, HttpStatus.NOT_FOUND, problem, msg(key, ex.messageArguments()), null);
	}

	/** EN: Tag missing by id or name. PT-BR: Etiqueta inexistente por id ou nome. */
	@ExceptionHandler(TagNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> tagNotFound(TagNotFoundException ex, HttpServletRequest request) {
		ApiProblemCode problem =
				ex.resolvedByUuid() ? ApiProblemCode.TAG_NOT_FOUND : ApiProblemCode.TAG_NOT_FOUND_BY_NAME;
		String key = ex.resolvedByUuid() ? "error.tag.notFound" : "error.tag.notFoundByName";
		return respond(request, HttpStatus.NOT_FOUND, problem, msg(key, ex.messageArguments()), null);
	}

	/** EN: Canonical tag name already taken. PT-BR: Nome de etiqueta já em uso. */
	@ExceptionHandler(DuplicateTagNameException.class)
	public ResponseEntity<ApiErrorResponse> duplicateTagName(DuplicateTagNameException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.CONFLICT,
				ApiProblemCode.TAG_NAME_CONFLICT,
				msg("error.tag.duplicateName", ex.getName()),
				null);
	}

	/** EN: User payload cited unknown catalogue tag ids → 400. PT-BR: Payload referiu ids de etiqueta inexistentes → 400. */
	@ExceptionHandler(UnknownTagReferencesException.class)
	public ResponseEntity<ApiErrorResponse> unknownTagRefs(UnknownTagReferencesException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.TAG_REFERENCES_INVALID,
				msg("error.tag.unknownIds", ex.formattedIds()),
				null);
	}

	/** EN: User public code already taken. PT-BR: Code de utilizador público já em uso. */
	@ExceptionHandler(DuplicateUserCodeException.class)
	public ResponseEntity<ApiErrorResponse> duplicateCode(DuplicateUserCodeException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.CONFLICT,
				ApiProblemCode.USER_CODE_CONFLICT,
				msg("error.user.duplicateCode", ex.getCode()),
				null);
	}

	/** EN: Tag set changed without doctor reference. PT-BR: Conjunto de tags alterado sem médico atribuídor. */
	@ExceptionHandler(TagAssignmentDoctorRequiredException.class)
	public ResponseEntity<ApiErrorResponse> tagAssignmentDoctorRequired(
			TagAssignmentDoctorRequiredException ex,
			HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.TAG_ASSIGNMENT_DOCTOR_REQUIRED,
				msg("error.user.tagAssignment.doctorRequired"),
				null);
	}

	/** EN: Acting doctor UUID not found. PT-BR: UUID do médico não encontrado. */
	@ExceptionHandler(AssigningDoctorNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> assigningDoctorNotFound(
			AssigningDoctorNotFoundException ex,
			HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.NOT_FOUND,
				ApiProblemCode.ASSIGNING_DOCTOR_NOT_FOUND,
				msg("error.user.tagAssignment.assigningDoctorNotFound", ex.doctorId().toString()),
				null);
	}

	/** EN: Referenced actor is not a doctor. PT-BR: Utilizador referenciado não é médico. */
	@ExceptionHandler(AssigningActorNotDoctorException.class)
	public ResponseEntity<ApiErrorResponse> assigningActorNotDoctor(
			AssigningActorNotDoctorException ex,
			HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.FORBIDDEN,
				ApiProblemCode.ASSIGNING_ACTOR_NOT_DOCTOR,
				msg("error.user.tagAssignment.actorNotDoctor", ex.actorCode()),
				null);
	}

	/** EN: Tags allowed only on patient accounts. PT-BR: Etiquetas só em contas paciente. */
	@ExceptionHandler(TagAssignmentPatientOnlyException.class)
	public ResponseEntity<ApiErrorResponse> tagAssignmentPatientOnly(
			TagAssignmentPatientOnlyException ex,
			HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.TAG_ASSIGNMENT_PATIENT_ONLY,
				msg("error.user.tagAssignment.patientOnly", ex.targetCode()),
				null);
	}

	/** EN: Acting clinician cites another clinician's attribution. PT-BR: Etiqueta já atribuída por outro médico. */
	@ExceptionHandler(TagHeldByOtherClinicianException.class)
	public ResponseEntity<ApiErrorResponse> tagHeldByOtherClinician(
			TagHeldByOtherClinicianException ex,
			HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.CONFLICT,
				ApiProblemCode.TAG_HELD_BY_OTHER_CLINICIAN,
				msg("error.user.tagAssignment.heldByOtherClinician", ex.catalogueTagId().toString()),
				null);
	}

	/** EN: Illegal argument from domain/service → 400. PT-BR: Argumento inválido na camada de domínio/serviço → 400. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.INVALID_ARGUMENT,
				msg("error.badRequest", ex.getMessage()),
				null);
	}

	/** EN: Path/param type mismatch → 400. PT-BR: Tipo incorreto de parâmetro/caminho → 400. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String param = ex.getName();
		Object val = ex.getValue();
		String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?";
		return respond(
				request,
				HttpStatus.BAD_REQUEST,
				ApiProblemCode.INVALID_ARGUMENT,
				msg(
						"error.argument.typeMismatch",
						param,
						val != null ? val.toString() : "",
						required),
				null);
	}

	/** EN: Bean Validation / MVC binding errors → 400 + fieldErrors. PT-BR: Bean Validation / binding MVC → 400 + fieldErrors. */
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

	/** EN: Catch-all mapped to sanitized 500 narrative. PT-BR: Último recurso → 500 com mensagem genérica segura. */
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
