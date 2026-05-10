package com.rb.multi.agent.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.SleepEntry;

/**
 * <p><b>EN:</b> Persistence for {@link com.rb.multi.agent.entity.SleepEntry} (patient sleep scale 0–10).</p>
 * <p><b>PT-BR:</b> Persistência de {@link com.rb.multi.agent.entity.SleepEntry} (escala de sono 0–10).</p>
 */
public interface SleepEntryRepository extends JpaRepository<SleepEntry, UUID> {

	List<SleepEntry> findByPatient_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
			UUID patientId, Instant fromInclusive, Instant toInclusive);
}
