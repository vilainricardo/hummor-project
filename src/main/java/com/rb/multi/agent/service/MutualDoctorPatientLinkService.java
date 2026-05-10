package com.rb.multi.agent.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.MutualDoctorCodeRequest;
import com.rb.multi.agent.dto.MutualDoctorPatientLinkResponse;
import com.rb.multi.agent.entity.Doctor;
import com.rb.multi.agent.entity.DoctorPatientMutualLink;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.DoctorPatientMutualLinkRepository;
import com.rb.multi.agent.repository.DoctorRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * <p><b>EN:</b> Mutual-consent pairing: patient and doctor each affirm the other via public {@code code}; roster membership
 * is granted only once both confirmations exist. The patient supplies {@link com.rb.multi.agent.dto.MutualDoctorCodeRequest#accessStartDate()}
 * (FR-004).</p>
 * <p><b>PT-BR:</b> Par mútuo: confirmações pelo {@code code}; a lista actualiza-se com ambas as metades. O paciente envia a data de
 * início de partilha (FR-004).</p>
 */
@Service
public class MutualDoctorPatientLinkService {

	private final UserRepository userRepository;
	private final DoctorRepository doctorRepository;
	private final DoctorPatientMutualLinkRepository mutualLinkRepository;

	public MutualDoctorPatientLinkService(
			UserRepository userRepository,
			DoctorRepository doctorRepository,
			DoctorPatientMutualLinkRepository mutualLinkRepository) {
		this.userRepository = userRepository;
		this.doctorRepository = doctorRepository;
		this.mutualLinkRepository = mutualLinkRepository;
	}

	/**
	 * EN: Patient {@code patientUserId} asserts intent toward the doctor in {@code body}; {@code body.accessStartDate()} defines
	 * inclusive data-access start (FR-004). PT-BR: Paciente confirma e define o início da partilha em {@code body.accessStartDate()}
	 * (FR-004).
	 */
	@Transactional
	public MutualDoctorPatientLinkResponse patientAcknowledgesDoctor(UUID patientUserId, MutualDoctorCodeRequest body) {
		User patient = userRepository.findById(patientUserId).orElseThrow(() -> UserNotFoundException.byId(patientUserId));
		Doctor doctor = resolveDoctorByPublicCode(body.doctorCode());
		assertNoSelfLink(patient, doctor);
		return acknowledgeAndMaybeFinalize(patient, doctor, true, false, body.accessStartDate());
	}

	/**
	 * EN: Doctor account {@code doctorUserId} asserts intent toward patient {@code patientPublicCode}. PT-BR: Conta-médico
	 * manifesta vínculo com o paciente do {@code patientPublicCode}.
	 */
	@Transactional
	public MutualDoctorPatientLinkResponse doctorAcknowledgesPatient(UUID doctorUserId, String patientPublicCodeRaw) {
		User acting = userRepository.findById(doctorUserId).orElseThrow(() -> UserNotFoundException.byId(doctorUserId));
		if (!(acting instanceof Doctor doctor)) {
			throw new IllegalArgumentException("acting user does not hold a doctor profile");
		}
		String patientLookupCode = normalizePublicCodeDisplay(patientPublicCodeRaw.trim());
		User patient =
				userRepository
						.findByCode(patientLookupCode)
						.orElseThrow(() -> UserNotFoundException.byCode(patientPublicCodeRaw.trim()));
		assertNoSelfLink(patient, doctor);
		return acknowledgeAndMaybeFinalize(patient, doctor, false, true, null);
	}

	private static void assertNoSelfLink(User patient, Doctor doctor) {
		if (patient.getId().equals(doctor.getId())) {
			throw new IllegalArgumentException("cannot link a user to themselves as doctor");
		}
	}

	private MutualDoctorPatientLinkResponse acknowledgeAndMaybeFinalize(
			User patient, Doctor doctor, boolean setPatientAck, boolean setDoctorAck, LocalDate accessStartDateFromPatient) {
		if (doctor.getPatients().contains(patient)) {
			return MutualDoctorPatientLinkResponse.alreadyOnRoster();
		}

		DoctorPatientMutualLink link =
				mutualLinkRepository
						.findByPatient_IdAndDoctor_Id(patient.getId(), doctor.getId())
						.orElseGet(
								() -> mutualLinkRepository.save(new DoctorPatientMutualLink(patient, doctor)));

		if (setPatientAck) {
			link.setPatientAcknowledged(true);
			link.setAccessStartDate(Objects.requireNonNull(accessStartDateFromPatient, "accessStartDate"));
		}
		if (setDoctorAck) {
			link.setDoctorAcknowledged(true);
		}
		mutualLinkRepository.save(link);

		if (!link.isFullyAcknowledged()) {
			return new MutualDoctorPatientLinkResponse(false, link.isPatientAcknowledged(), link.isDoctorAcknowledged());
		}

		LocalDate accessStart = Objects.requireNonNull(link.getAccessStartDate(), "accessStartDate");
		Doctor doctorPersisted =
				doctorRepository
						.findById(doctor.getId())
						.orElseThrow(() -> new IllegalStateException("doctor aggregate missing for id=" + doctor.getId()));
		User patientPersisted =
				userRepository.findById(patient.getId()).orElseThrow(() -> UserNotFoundException.byId(patient.getId()));
		doctorPersisted.addPatient(patientPersisted, accessStart);
		doctorRepository.save(doctorPersisted);
		mutualLinkRepository.delete(link);
		return MutualDoctorPatientLinkResponse.alreadyOnRoster();
	}

	private Doctor resolveDoctorByPublicCode(String doctorPublicCodeRaw) {
		String code = normalizePublicCodeDisplay(doctorPublicCodeRaw);
		User found = userRepository.findByCode(code).orElseThrow(() -> UserNotFoundException.byCode(doctorPublicCodeRaw.trim()));
		if (!(found instanceof Doctor doctor)) {
			throw new IllegalArgumentException("user with this code does not have a doctor profile");
		}
		return doctor;
	}

	private static String normalizePublicCodeDisplay(String raw) {
		String trimmed = Objects.requireNonNull(raw, "code").trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("code must not be blank");
		}
		if (trimmed.length() > 20) {
			throw new IllegalArgumentException("code must be at most 20 characters");
		}
		return trimmed;
	}
}
