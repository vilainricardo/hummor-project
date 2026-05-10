package com.rb.multi.agent.dto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.rb.multi.agent.entity.MoodEntry;
import com.rb.multi.agent.entity.SleepEntry;

/**
 * <p><b>EN:</b> One mood or sleep scale reading exposed to clinicians (no PII beyond linkage).</p>
 * <p><b>PT-BR:</b> Uma leitura de escala humor/sono exposta a clínicos.</p>
 */
public record ScaleEntryResponse(UUID id, String kind, int value, Instant recordedAt) {

	public static ScaleEntryResponse fromMood(MoodEntry e) {
		return new ScaleEntryResponse(e.getId(), "MOOD", e.getValue(), e.getCreatedAt());
	}

	public static ScaleEntryResponse fromSleep(SleepEntry e) {
		Instant anchor = e.getRecordedOn().atStartOfDay(ZoneOffset.UTC).toInstant();
		return new ScaleEntryResponse(e.getId(), "SLEEP", e.getValue(), anchor);
	}
}
