package com.rb.multi.agent.entity;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> Clinician account via JPA JOINED inheritance: same surrogate key as {@link User}, extra row in {@code doctors}
 * and a many-to-many patient roster for now.</p>
 * <p><b>PT-BR:</b> Conta de médico com herança JOINED: mesma chave que {@link User}, linha extra em {@code doctors} e lista
 * de pacientes (N:N) por hora.</p>
 */
@Entity
@Table(name = "doctors")
public class Doctor extends User {

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "doctor_patients",
			joinColumns = @JoinColumn(name = "doctor_id", nullable = false),
			inverseJoinColumns = @JoinColumn(name = "patient_user_id", nullable = false))
	private Set<User> patients = new LinkedHashSet<>();

	protected Doctor() {
	}

	/** EN: Persisted clinician; {@link #isDoctor()} is always {@code true}. PT-BR: Médico persistível; sempre {@code isDoctor=true}. */
	public Doctor(String code) {
		super(code, true);
	}

	public Set<User> getPatients() {
		return patients;
	}

	/** EN: Adds a patient to this doctor’s roster. PT-BR: Adiciona paciente ao conjunto. */
	public void addPatient(User patient) {
		patients.add(Objects.requireNonNull(patient, "patient"));
	}

	/** EN: Removes a patient link. PT-BR: Remove ligação ao paciente. */
	public void removePatient(User patient) {
		patients.remove(patient);
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
