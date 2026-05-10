package com.rb.multi.agent.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.ScaleEntryResponse;
import com.rb.multi.agent.entity.MoodEntry;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.MoodEntryTooSoonException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.MoodEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * <p><b>EN:</b> Patient mood capture (FR-005) with a minimum spacing between entries.</p>
 * <p><b>PT-BR:</b> Registo de humor pelo paciente (FR-005) com intervalo mínimo entre entradas.</p>
 */
@Service
public class MoodEntryService {

	/** EN: Minimum elapsed wall time since the previous entry before another is accepted. PT-BR: Intervalo mínimo entre registos. */
	public static final Duration MIN_INTERVAL_BETWEEN_ENTRIES = Duration.ofMinutes(1);

	private final UserRepository userRepository;
	private final MoodEntryRepository moodEntryRepository;

	public MoodEntryService(UserRepository userRepository, MoodEntryRepository moodEntryRepository) {
		this.userRepository = userRepository;
		this.moodEntryRepository = moodEntryRepository;
	}

	/**
	 * <p><b>EN:</b> Persists one mood row for {@code patientId} if none exists in the last minute.</p>
	 * <p><b>PT-BR:</b> Grava humor se não houver outro registo no último minuto.</p>
	 */
	@Transactional
	public ScaleEntryResponse registerMood(UUID patientId, int value) {
		User patient =
				userRepository.findByIdForUpdate(patientId).orElseThrow(() -> UserNotFoundException.byId(patientId));
		Instant since = Instant.now().minus(MIN_INTERVAL_BETWEEN_ENTRIES);
		if (moodEntryRepository.existsByPatient_IdAndCreatedAtGreaterThanEqual(patientId, since)) {
			throw new MoodEntryTooSoonException(patientId);
		}
		MoodEntry saved = moodEntryRepository.save(new MoodEntry(patient, value));
		return ScaleEntryResponse.fromMood(saved);
	}
}
