package com.rb.multi.agent.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

/**
 * <p><b>EN:</b> Body for registering sleep for a chosen calendar day (UTC).</p>
 * <p><b>PT-BR:</b> Corpo para registar sono num dia civil (UTC) escolhido.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SleepEntryForDateRequest(
		@NotNull(message = "{validation.sleep.value.required}")
		@Min(value = 0, message = "{validation.sleep.value.range}")
		@Max(value = 10, message = "{validation.sleep.value.range}")
		Integer value,
		@NotNull(message = "{validation.sleep.date.required}")
		@PastOrPresent(message = "{validation.sleep.date.pastOrPresent}")
		LocalDate date) {
}
