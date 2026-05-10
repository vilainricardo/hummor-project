package com.rb.multi.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * <p><b>EN:</b> Patient-submitted mood score (0–10).</p>
 * <p><b>PT-BR:</b> Valor de humor submetido pelo paciente (0–10).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record MoodEntryCreateRequest(
		@NotNull(message = "{validation.mood.value.required}")
		@Min(value = 0, message = "{validation.mood.value.range}")
		@Max(value = 10, message = "{validation.mood.value.range}")
		Integer value) {
}
