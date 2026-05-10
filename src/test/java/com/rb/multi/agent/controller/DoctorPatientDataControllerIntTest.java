package com.rb.multi.agent.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rb.multi.agent.entity.Doctor;
import com.rb.multi.agent.entity.DoctorPatientAssociation;
import com.rb.multi.agent.entity.DoctorPatientKey;
import com.rb.multi.agent.entity.DoctorPatientLinkStatus;
import com.rb.multi.agent.entity.MoodEntry;
import com.rb.multi.agent.entity.SleepEntry;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.repository.DoctorPatientAssociationRepository;
import com.rb.multi.agent.repository.DoctorRepository;
import com.rb.multi.agent.repository.MoodEntryRepository;
import com.rb.multi.agent.repository.SleepEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

class DoctorPatientDataControllerIntTest extends AbstractControllerIntTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DoctorRepository doctorRepository;

	@Autowired
	private MoodEntryRepository moodEntryRepository;

	@Autowired
	private SleepEntryRepository sleepEntryRepository;

	@Autowired
	private DoctorPatientAssociationRepository associationRepository;

	@BeforeEach
	void purge() {
		moodEntryRepository.deleteAll();
		sleepEntryRepository.deleteAll();
		associationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("GET mood/sleep/history — 200 quando há vínculo e dados dentro do consentimento")
	void linkedDoctor_readsMoodAndSleep() throws Exception {
		var pair = saveLinkedPair("doc-scale", "pat-scale", LocalDate.of(2020, 1, 1));
		moodEntryRepository.save(new MoodEntry(pair.patient(), 6));
		sleepEntryRepository.save(new SleepEntry(pair.patient(), 8, LocalDate.of(2025, 6, 10)));

		mockMvc.perform(get(apiMood(pair.doctorUserId(), pair.patientId()))).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());

		mockMvc.perform(get(apiSleep(pair.doctorUserId(), pair.patientId()))).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());

		mockMvc.perform(get(apiHistory(pair.doctorUserId(), pair.patientId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.patientAccessStartDate").value("2020-01-01"))
				.andExpect(jsonPath("$.moodEntries", hasSize(1)))
				.andExpect(jsonPath("$.sleepEntries", hasSize(1)));
	}

	@Test
	@DisplayName("utente id no path não é médico → 403")
	void actorNotDoctorProfile_forbidden() throws Exception {
		var pair = saveLinkedPair("doc-403", "pat-403", LocalDate.of(2020, 1, 1));
		User notDoctor = userRepository.save(User.seedPatient("not-doc-scale"));
		mockMvc.perform(get(apiMood(notDoctor.getId(), pair.patientId()))).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("sem vínculo na lista → 404")
	void notLinked_notFound() throws Exception {
		Doctor doc = (Doctor) userRepository.save(User.seedClinician("doc-nolink"));
		User stranger = userRepository.save(User.seedPatient("stranger-scale"));
		mockMvc.perform(get(apiMood(doc.getId(), stranger.getId()))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("UNLINKED_WITHOUT_ACCESS → 403")
	void revoked_forbidden() throws Exception {
		var pair = saveLinkedPair("doc-rev", "pat-rev", LocalDate.of(2020, 1, 1));
		DoctorPatientAssociation assoc =
				associationRepository.findById(new DoctorPatientKey(pair.doctorUserId(), pair.patientId())).orElseThrow();
		assoc.setStatus(DoctorPatientLinkStatus.UNLINKED_WITHOUT_ACCESS);
		associationRepository.save(assoc);

		mockMvc.perform(get(apiMood(pair.doctorUserId(), pair.patientId()))).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("janela from/to exclui fora do pedido; consentimento limita o início")
	void periodQuery_respectsBounds() throws Exception {
		var pair = saveLinkedPair("doc-win", "pat-win", LocalDate.of(2025, 6, 1));
		moodEntryRepository.save(new MoodEntry(pair.patient(), 3));

		Instant from = LocalDate.of(2025, 5, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		Instant to = LocalDate.of(2025, 5, 31).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		mockMvc.perform(get(apiMood(pair.doctorUserId(), pair.patientId())).param("from", from.toString()).param("to", to.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		Instant fromOk = LocalDate.of(2025, 6, 2).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		mockMvc.perform(get(apiMood(pair.doctorUserId(), pair.patientId())).param("from", fromOk.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	private static String apiMood(UUID doctorId, UUID patientId) {
		return "/api/v1/doctors/" + doctorId + "/patients/" + patientId + "/mood-entries";
	}

	private static String apiSleep(UUID doctorId, UUID patientId) {
		return "/api/v1/doctors/" + doctorId + "/patients/" + patientId + "/sleep-entries";
	}

	private static String apiHistory(UUID doctorId, UUID patientId) {
		return "/api/v1/doctors/" + doctorId + "/patients/" + patientId + "/scale-history";
	}

	private LinkedPair saveLinkedPair(String docCode, String patCode, LocalDate accessStart) {
		Doctor doc = (Doctor) userRepository.save(User.seedClinician(docCode));
		User pat = userRepository.save(User.seedPatient(patCode));
		doc.addPatient(pat, accessStart);
		doctorRepository.save(doc);
		return new LinkedPair(doc.getId(), pat.getId(), pat);
	}

	private record LinkedPair(UUID doctorUserId, UUID patientId, User patient) {
	}
}
