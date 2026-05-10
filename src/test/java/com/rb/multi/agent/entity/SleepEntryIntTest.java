package com.rb.multi.agent.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.repository.SleepEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

@SpringBootTest
@Transactional
class SleepEntryIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SleepEntryRepository sleepEntryRepository;

	@BeforeEach
	void purge() {
		sleepEntryRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("sono 0–10 persiste com paciente e created_at")
	void roundTrip_defaultCreatedAt() {
		User p = userRepository.save(User.seedPatient("sleep-pat-it"));
		var entry = new SleepEntry(p, 7, LocalDate.now(ZoneOffset.UTC));
		SleepEntry saved = sleepEntryRepository.save(entry);
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getValue()).isEqualTo(7);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getPatient().getId()).isEqualTo(p.getId());
		assertThat(saved.getRecordedOn()).isNotNull();
	}

	@Test
	@DisplayName("valor fora de 0–10 → IllegalArgumentException")
	void badScore_throws() {
		User p = userRepository.save(User.seedPatient("sleep-bad-it"));
		LocalDate d = LocalDate.of(2025, 3, 1);
		assertThatThrownBy(() -> new SleepEntry(p, 11, d)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SleepEntry(p, -1, d)).isInstanceOf(IllegalArgumentException.class);
	}
}
