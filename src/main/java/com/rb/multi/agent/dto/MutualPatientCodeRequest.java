package com.rb.multi.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Doctor asserts intent to pair with the patient identified by {@code patientCode}.</p>
 * <p><b>PT-BR:</b> Médico indica vínculo com o paciente identificado pelo {@code patientCode}.</p>
 */
public record MutualPatientCodeRequest(
		@NotBlank(message = "{validation.user.code.blank}")
		@Size(max = 20, message = "{validation.user.code.size}")
		String patientCode) {
}
