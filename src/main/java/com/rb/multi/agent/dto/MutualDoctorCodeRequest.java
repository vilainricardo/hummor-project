package com.rb.multi.agent.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p><b>EN:</b> Patient asserts intent to pair with the doctor identified by {@code doctorCode}; {@code accessStartDate} is the
 * inclusive start of shared data access (FR-004).</p>
 * <p><b>PT-BR:</b> Paciente indica vínculo com o médico identificado pelo {@code doctorCode}; {@code accessStartDate} é o início
 * (inclusive) da partilha de dados (FR-004).</p>
 */
public record MutualDoctorCodeRequest(
		@NotBlank(message = "{validation.user.code.blank}")
		@Size(max = 20, message = "{validation.user.code.size}")
		String doctorCode,
		@NotNull(message = "{validation.mutualLink.accessStartDate.required}")
		LocalDate accessStartDate) {
}
