package com.rb.multi.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * <p><b>EN:</b> Body for registering sleep for the current UTC calendar day.</p>
 * <p><b>PT-BR:</b> Corpo para registar sono no dia civil UTC actual.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SleepEntryTodayRequest(
		@NotNull(message = "{validation.sleep.value.required}")
		@Min(value = 0, message = "{validation.sleep.value.range}")
		@Max(value = 10, message = "{validation.sleep.value.range}")
		Integer value) {
}
