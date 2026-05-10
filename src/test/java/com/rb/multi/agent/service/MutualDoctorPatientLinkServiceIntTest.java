package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.MutualDoctorCodeRequest;
import com.rb.multi.agent.dto.MutualDoctorPatientLinkResponse;
import com.rb.multi.agent.entity.Doctor;
import com.rb.multi.agent.entity.DoctorPatientKey;
import com.rb.multi.agent.constants.DoctorPatientLinkStatus;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.DoctorPatientAssociationRepository;
import com.rb.multi.agent.repository.DoctorPatientMutualLinkRepository;
import com.rb.multi.agent.repository.DoctorRepository;
import com.rb.multi.agent.repository.UserRepository;

@SpringBootTest
@Transactional
class MutualDoctorPatientLinkServiceIntTest {

	/** EN: Patient-defined access start used in mutual-link requests. PT-BR: Data de partilha usada nos testes de vínculo. */
	private static final LocalDate ACCESS_START = LocalDate.of(2026, 5, 1);

	@Autowired
	private MutualDoctorPatientLinkService mutualDoctorPatientLinkService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DoctorRepository doctorRepository;

	@Autowired
	private DoctorPatientMutualLinkRepository mutualLinkRepository;

	@Autowired
	private DoctorPatientAssociationRepository doctorPatientAssociationRepository;

	@BeforeEach
	void purge() {
		mutualLinkRepository.deleteAll();
		doctorPatientAssociationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("paciente primeiro, médico segundo → lista do médico recebe paciente")
	void patientFirst_thenDoctor_completesRoster() {
		User patient = userRepository.save(User.seedPatient("pat-mutual-a"));
		Doctor doc = (Doctor) userRepository.save(User.seedClinician("doc-mutual-a"));

		MutualDoctorPatientLinkResponse r1 =
				mutualDoctorPatientLinkService.patientAcknowledgesDoctor(
						patient.getId(), new MutualDoctorCodeRequest("doc-mutual-a", ACCESS_START));
		assertThat(r1.rosterLinkedNow()).isFalse();
		assertThat(r1.patientAcknowledged()).isTrue();
		assertThat(r1.doctorAcknowledged()).isFalse();

		MutualDoctorPatientLinkResponse r2 =
				mutualDoctorPatientLinkService.doctorAcknowledgesPatient(doc.getId(), "pat-mutual-a");
		assertThat(r2.rosterLinkedNow()).isTrue();
		assertThat(r2.patientAcknowledged()).isTrue();
		assertThat(r2.doctorAcknowledged()).isTrue();

		Doctor refreshed = doctorRepository.findById(doc.getId()).orElseThrow();
		assertThat(refreshed.getPatients()).extracting(User::getCode).containsExactly("pat-mutual-a");
		var assoc =
				doctorPatientAssociationRepository.findById(new DoctorPatientKey(doc.getId(), patient.getId())).orElseThrow();
		assertThat(assoc.getAccessStartDate()).isEqualTo(ACCESS_START);
		assertThat(assoc.getStatus()).isEqualTo(DoctorPatientLinkStatus.ACTIVE);
		assertThat(mutualLinkRepository.count()).isZero();
	}

	@Test
	@DisplayName("médico primeiro, paciente segundo → mesmo resultado")
	void doctorFirst_thenPatient_completesRoster() {
		User patient = userRepository.save(User.seedPatient("pat-mutual-b"));
		Doctor doc = (Doctor) userRepository.save(User.seedClinician("doc-mutual-b"));

		MutualDoctorPatientLinkResponse r1 =
				mutualDoctorPatientLinkService.doctorAcknowledgesPatient(doc.getId(), "pat-mutual-b");
		assertThat(r1.rosterLinkedNow()).isFalse();
		assertThat(r1.patientAcknowledged()).isFalse();
		assertThat(r1.doctorAcknowledged()).isTrue();

		MutualDoctorPatientLinkResponse r2 =
				mutualDoctorPatientLinkService.patientAcknowledgesDoctor(
						patient.getId(), new MutualDoctorCodeRequest("doc-mutual-b", ACCESS_START));
		assertThat(r2.rosterLinkedNow()).isTrue();

		Doctor done = doctorRepository.findById(doc.getId()).orElseThrow();
		assertThat(done.getPatients()).extracting(User::getCode).containsExactly("pat-mutual-b");
		var assoc =
				doctorPatientAssociationRepository.findById(new DoctorPatientKey(doc.getId(), patient.getId())).orElseThrow();
		assertThat(assoc.getAccessStartDate()).isEqualTo(ACCESS_START);
		assertThat(assoc.getStatus()).isEqualTo(DoctorPatientLinkStatus.ACTIVE);
	}

	@Test
	@DisplayName("code aponta para utilizador sem perfil Doctor → IllegalArgumentException")
	void nonDoctorCode_throws() {
		User patient = userRepository.save(User.seedPatient("pat-mutual-c"));
		userRepository.save(User.seedPatient("not-a-doc"));
		assertThatThrownBy(
						() ->
								mutualDoctorPatientLinkService.patientAcknowledgesDoctor(
										patient.getId(), new MutualDoctorCodeRequest("not-a-doc", ACCESS_START)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("doctor profile");
	}

	@Test
	@DisplayName("UUID no path que não é Doctor → IllegalArgumentException")
	void actingUserNotDoctorProfile_throws() {
		User patient = userRepository.save(User.seedPatient("pat-mutual-d"));
		assertThatThrownBy(() -> mutualDoctorPatientLinkService.doctorAcknowledgesPatient(patient.getId(), "pat-mutual-d"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("doctor profile");
	}

	@Test
	@DisplayName("paciente inexistente por code → UserNotFoundException")
	void unknownPatientCode_throws() {
		Doctor doc = (Doctor) userRepository.save(User.seedClinician("doc-mutual-e"));
		assertThatThrownBy(() -> mutualDoctorPatientLinkService.doctorAcknowledgesPatient(doc.getId(), "ghost-patient"))
				.isInstanceOf(UserNotFoundException.class);
	}
}
