package com.rb.multi.agent.entity;

import java.time.LocalDate;
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
 * public {@code code} before the patient appears in {@link Doctor#getPatients()}. {@link #accessStartDate} is set when the
 * <em>patient</em> acknowledges (FR-004) and is copied to {@code doctor_patients} when the link completes.</p>
 * <p><b>PT-BR:</b> Ligação pendente paciente–médico; ambos confirmam pelo {@code code}. A {@link #accessStartDate} é definida
 * pelo passo do <em>paciente</em> (FR-004) e copiada para {@code doctor_patients} ao completar.</p>
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

	/**
	 * EN: Inclusive date from which the doctor may view patient data; set on patient acknowledgement (FR-004).
	 * PT-BR: Data inclusive a partir da qual o médico pode ver dados do paciente; definida na confirmação do paciente (FR-004).
	 */
	@Column(name = "access_start_date")
	private LocalDate accessStartDate;

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

	public LocalDate getAccessStartDate() {
		return accessStartDate;
	}

	public void setAccessStartDate(LocalDate accessStartDate) {
		this.accessStartDate = accessStartDate;
	}
}
