package com.rb.multi.agent.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.repository.MoodEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

@SpringBootTest
@Transactional
class MoodEntryIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MoodEntryRepository moodEntryRepository;

	@BeforeEach
	void purge() {
		moodEntryRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("humor 0–10 persiste com paciente e created_at")
	void roundTrip_defaultCreatedAt() {
		User p = userRepository.save(User.seedPatient("mood-pat-it"));
		var entry = new MoodEntry(p, 4);
		MoodEntry saved = moodEntryRepository.save(entry);
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getValue()).isEqualTo(4);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getPatient().getId()).isEqualTo(p.getId());
	}

	@Test
	@DisplayName("valor fora de 0–10 → IllegalArgumentException")
	void badScore_throws() {
		User p = userRepository.save(User.seedPatient("mood-bad-it"));
		assertThatThrownBy(() -> new MoodEntry(p, 11)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MoodEntry(p, -1)).isInstanceOf(IllegalArgumentException.class);
	}
}
