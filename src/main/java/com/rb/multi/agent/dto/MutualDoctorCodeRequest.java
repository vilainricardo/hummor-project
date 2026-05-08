package com.rb.multi.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Patient asserts intent to pair with the doctor identified by {@code doctorCode}.</p>
 * <p><b>PT-BR:</b> Paciente indica vínculo com o médico identificado pelo {@code doctorCode}.</p>
 */
public record MutualDoctorCodeRequest(
		@NotBlank(message = "{validation.user.code.blank}")
		@Size(max = 20, message = "{validation.user.code.size}")
		String doctorCode) {
}
