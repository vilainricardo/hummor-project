package com.rb.multi.agent.entity;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> Clinician account via JPA JOINED inheritance: same surrogate key as {@link User}, extra row in {@code doctors}
 * and roster rows in {@code doctor_patients} ({@link DoctorPatientAssociation}) with FR-004 access start and FR-003 status.</p>
 * <p><b>PT-BR:</b> Conta de médico; linhas em {@code doctor_patients} com data de acesso (FR-004) e estado do vínculo (FR-003).</p>
 */
@Entity
@Table(name = "doctors")
public class Doctor extends User {

	@OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<DoctorPatientAssociation> patientRoster = new LinkedHashSet<>();

	protected Doctor() {
	}

	/** EN: Persisted clinician; {@link #isDoctor()} is always {@code true}. PT-BR: Médico persistível; sempre {@code isDoctor=true}. */
	public Doctor(String code) {
		super(code, true);
	}

	/**
	 * EN: Distinct patients on the roster (join snapshot only). PT-BR: Pacientes distintos na lista (apenas leitura).
	 */
	public Set<User> getPatients() {
		return patientRoster.stream().map(DoctorPatientAssociation::getPatient).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * EN: Adds a roster membership with the patient-defined access start date (FR-004). PT-BR: Adiciona entrada com data de início
	 * de partilha definida pelo paciente (FR-004).
	 */
	public void addPatient(User patient, LocalDate accessStartDate) {
		patientRoster.add(new DoctorPatientAssociation(this, patient, accessStartDate));
	}

	/** EN: Removes a patient link. PT-BR: Remove ligação ao paciente. */
	public void removePatient(User patient) {
		Objects.requireNonNull(patient, "patient");
		patientRoster.removeIf(a -> a.getPatient().getId().equals(patient.getId()));
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
