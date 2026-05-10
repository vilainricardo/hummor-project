package com.rb.multi.agent.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.repository.DoctorRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * EN: {@link Doctor} JOINED subclass mapping smoke test.
 * PT-BR: Teste do mapeamento da subtipo {@link Doctor} (JOINED).
 */
@SpringBootTest
@Transactional
class DoctorEntityIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DoctorRepository doctorRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void purge() {
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("Doctor extends User; mesmo id em users/doctors; pacientes na N:N")
	void doctorJoined_roundTripAndPatients() {
		Doctor clinician = new Doctor("doc-entity-it");
		clinician.setEmail(User.integrationSeedEmail("doc-entity-it"));
		clinician = userRepository.save(clinician);

		User p1 = userRepository.save(User.seedPatient("pat-a-it"));
		User p2 = userRepository.save(User.seedPatient("pat-b-it"));
		LocalDate d0 = LocalDate.of(2026, 3, 1);
		clinician.addPatient(p1, d0);
		clinician.addPatient(p2, d0.plusDays(1));
		clinician = doctorRepository.save(clinician);

		assertThat(clinician.getId()).isNotNull();
		assertThat(clinician.getPatients()).extracting(User::getCode).containsExactlyInAnyOrder("pat-a-it", "pat-b-it");

		UUID clinicianId = clinician.getId();
		entityManager.flush();
		entityManager.clear();

		User polymorphicReload = userRepository.findById(clinicianId).orElseThrow();
		assertThat(polymorphicReload).isInstanceOf(Doctor.class);
		assertThat(((Doctor) polymorphicReload).getPatients()).hasSize(2);

		assertThat(doctorRepository.findById(clinicianId)).isPresent();
	}
}
