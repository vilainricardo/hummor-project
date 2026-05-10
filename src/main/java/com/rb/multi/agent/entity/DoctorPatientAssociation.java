package com.rb.multi.agent.entity;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> Doctor–patient roster row: FR-004 {@code access_start_date} and FR-003 {@link #status} (unlink variants).</p>
 * <p><b>PT-BR:</b> Linha médico–paciente: {@code access_start_date} (FR-004) e {@link #status} do desvincular (FR-003).</p>
 */
@Entity
@Table(name = "doctor_patients")
public class DoctorPatientAssociation {

	@EmbeddedId
	private DoctorPatientKey id;

	@MapsId("doctorId")
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@MapsId("patientUserId")
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_user_id", nullable = false)
	private User patient;

	@Column(name = "access_start_date", nullable = false)
	private LocalDate accessStartDate;

	@Column(name = "status", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private DoctorPatientLinkStatus status = DoctorPatientLinkStatus.ACTIVE;

	protected DoctorPatientAssociation() {
	}

	/**
	 * EN: Both accounts must already have surrogate ids (persisted). New links start {@link DoctorPatientLinkStatus#ACTIVE} (FR-003).
	 * PT-BR: Novos vínculos começam em ACTIVE (FR-003).
	 */
	public DoctorPatientAssociation(Doctor doctor, User patient, LocalDate accessStartDate) {
		this.doctor = Objects.requireNonNull(doctor, "doctor");
		this.patient = Objects.requireNonNull(patient, "patient");
		this.accessStartDate = Objects.requireNonNull(accessStartDate, "accessStartDate");
		this.status = DoctorPatientLinkStatus.ACTIVE;
		this.id = new DoctorPatientKey(doctor.getId(), patient.getId());
	}

	public DoctorPatientKey getId() {
		return id;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	public User getPatient() {
		return patient;
	}

	public LocalDate getAccessStartDate() {
		return accessStartDate;
	}

	public DoctorPatientLinkStatus getStatus() {
		return status;
	}

	/**
	 * EN: FR-003 state transition (authorization rules belong in the service layer). PT-BR: Transição de estado FR-003.
	 */
	public void setStatus(DoctorPatientLinkStatus status) {
		this.status = Objects.requireNonNull(status, "status");
	}
}
