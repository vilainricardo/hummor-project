package com.rb.multi.agent.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.SleepEntry;

/**
 * <p><b>EN:</b> Persistence for {@link com.rb.multi.agent.entity.SleepEntry} (patient sleep scale 0–10).</p>
 * <p><b>PT-BR:</b> Persistência de {@link com.rb.multi.agent.entity.SleepEntry} (escala de sono 0–10).</p>
 */
public interface SleepEntryRepository extends JpaRepository<SleepEntry, UUID> {

	/** EN: Sleep diary rows for {@code recordedOn} in [{@code fromInclusive}, {@code toInclusive}] (inclusive calendar days). */
	List<SleepEntry> findByPatient_IdAndRecordedOnBetweenOrderByRecordedOnDesc(
			UUID patientId, LocalDate fromInclusive, LocalDate toInclusive);

	boolean existsByPatient_IdAndRecordedOn(UUID patientId, LocalDate recordedOn);
}
