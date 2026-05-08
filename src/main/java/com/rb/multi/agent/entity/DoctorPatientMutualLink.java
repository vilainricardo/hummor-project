package com.rb.multi.agent.entity;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * <p><b>EN:</b> Pending mutual link between one patient-shaped {@link User} and one {@link Doctor}; both sides must confirm by
 * public {@code code} before the patient appears in {@link Doctor#getPatients()}.</p>
 * <p><b>PT-BR:</b> Ligação pendente entre um {@link User} paciente e um {@link Doctor}; ambos confirmam pelo {@code code}
 * antes do paciente entrar em {@link Doctor#getPatients()}.</p>
 */
@Entity
@Table(
		name = "doctor_patient_mutual_links",
		uniqueConstraints = @UniqueConstraint(name = "uk_mutual_doctor_patient", columnNames = {"patient_user_id", "doctor_user_id"}))
public class DoctorPatientMutualLink {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_user_id", nullable = false)
	private User patient;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "doctor_user_id", nullable = false, referencedColumnName = "id")
	private Doctor doctor;

	@Column(name = "patient_acknowledged", nullable = false)
	private boolean patientAcknowledged;

	@Column(name = "doctor_acknowledged", nullable = false)
	private boolean doctorAcknowledged;

	protected DoctorPatientMutualLink() {
	}

	public DoctorPatientMutualLink(User patient, Doctor doctor) {
		this.patient = Objects.requireNonNull(patient, "patient");
		this.doctor = Objects.requireNonNull(doctor, "doctor");
		if (patient.getId() != null
				&& doctor.getId() != null
				&& patient.getId().equals(doctor.getId())) {
			throw new IllegalArgumentException("patient and doctor must be distinct users");
		}
	}

	public UUID getId() {
		return id;
	}

	public User getPatient() {
		return patient;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	public boolean isPatientAcknowledged() {
		return patientAcknowledged;
	}

	public void setPatientAcknowledged(boolean patientAcknowledged) {
		this.patientAcknowledged = patientAcknowledged;
	}

	public boolean isDoctorAcknowledged() {
		return doctorAcknowledged;
	}

	public void setDoctorAcknowledged(boolean doctorAcknowledged) {
		this.doctorAcknowledged = doctorAcknowledged;
	}

	public boolean isFullyAcknowledged() {
		return patientAcknowledged && doctorAcknowledged;
	}
}
