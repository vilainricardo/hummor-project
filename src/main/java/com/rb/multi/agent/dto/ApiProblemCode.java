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
	/** EN: One or more tag UUIDs absent from catalogue. PT-BR: Um ou mais UUIDs de tag inexistentes no catálogo. */
	TAG_REFERENCES_INVALID,
	/** EN: Bean validation failed. PT-BR: Falha de validação Bean Validation. */
	VALIDATION_FAILED,
	/** EN: Illegal argument or bad input. PT-BR: Argumento inválido ou entrada incorreta. */
	INVALID_ARGUMENT,
	/** EN: Patient tag mutation without assigning doctor identity. PT-BR: Mudança de tags sem identificar o médico atribuídor. */
	TAG_ASSIGNMENT_DOCTOR_REQUIRED,
	/** EN: Assigning-doctor UUID not persisted. PT-BR: UUID do médico atribuídor inexistente. */
	ASSIGNING_DOCTOR_NOT_FOUND,
	/** EN: Assigned-by user exists but lacks doctor role. PT-BR: Utente existe mas não tem perfil médico. */
	ASSIGNING_ACTOR_NOT_DOCTOR,
	/** EN: Patient-only tag links; target account is clinician. PT-BR: Etiquetas só para paciente; conta alvo é clínico. */
	TAG_ASSIGNMENT_PATIENT_ONLY,
	/** EN: Unhandled server failure. PT-BR: Falha interna não tratada. */
	INTERNAL_ERROR
}
