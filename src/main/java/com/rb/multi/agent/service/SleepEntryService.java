package com.rb.multi.agent.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.ScaleEntryResponse;
import com.rb.multi.agent.entity.SleepEntry;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.SleepEntryDayConflictException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.SleepEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * <p><b>EN:</b> Patient sleep capture (FR-006) — at most one row per patient per UTC calendar day.</p>
 * <p><b>PT-BR:</b> Registo de sono (FR-006) — no máximo uma linha por paciente e dia civil UTC.</p>
 */
@Service
public class SleepEntryService {

	private final UserRepository userRepository;
	private final SleepEntryRepository sleepEntryRepository;

	public SleepEntryService(UserRepository userRepository, SleepEntryRepository sleepEntryRepository) {
		this.userRepository = userRepository;
		this.sleepEntryRepository = sleepEntryRepository;
	}

	@Transactional
	public ScaleEntryResponse registerForToday(UUID patientId, int value) {
		LocalDate day = LocalDate.now(ZoneOffset.UTC);
		return registerForDay(patientId, value, day);
	}

	@Transactional
	public ScaleEntryResponse registerForDay(UUID patientId, int value, LocalDate recordedOn) {
		User patient =
				userRepository.findByIdForUpdate(patientId).orElseThrow(() -> UserNotFoundException.byId(patientId));
		if (sleepEntryRepository.existsByPatient_IdAndRecordedOn(patientId, recordedOn)) {
			throw new SleepEntryDayConflictException(patientId, recordedOn);
		}
		SleepEntry saved = sleepEntryRepository.save(new SleepEntry(patient, value, recordedOn));
		return ScaleEntryResponse.fromSleep(saved);
	}
}
