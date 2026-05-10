package com.rb.multi.agent.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.MoodEntry;

/**
 * <p><b>EN:</b> Persistence for {@link com.rb.multi.agent.entity.MoodEntry} (patient mood scale 0–10).</p>
 * <p><b>PT-BR:</b> Persistência de {@link com.rb.multi.agent.entity.MoodEntry} (escala de humor 0–10).</p>
 */
public interface MoodEntryRepository extends JpaRepository<MoodEntry, UUID> {

	List<MoodEntry> findByPatient_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
			UUID patientId, Instant fromInclusive, Instant toInclusive);

	/** EN: Any mood entry for this patient at or after {@code sinceInclusive}. PT-BR: Registo de humor ≥ instante. */
	boolean existsByPatient_IdAndCreatedAtGreaterThanEqual(UUID patientId, Instant sinceInclusive);
}
