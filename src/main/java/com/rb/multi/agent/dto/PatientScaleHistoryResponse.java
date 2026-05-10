package com.rb.multi.agent.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * <p><b>EN:</b> Mood + sleep in one payload with the effective query window (patient consent and request params).
 * {@code patientAccessStartDate} is FR-004; {@code queryFromInclusive}/{@code queryToInclusive} reflect consent clamping.</p>
 * <p><b>PT-BR:</b> Humor e sono com intervalo efectivo. {@code patientAccessStartDate} é FR-004; os {@code query*} reflectem o clamp.</p>
 */
public record PatientScaleHistoryResponse(
		LocalDate patientAccessStartDate,
		Instant queryFromInclusive,
		Instant queryToInclusive,
		List<ScaleEntryResponse> moodEntries,
		List<ScaleEntryResponse> sleepEntries,
		List<TrendIndicatorResponse> trendIndicators) {
}
