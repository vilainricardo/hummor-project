package com.rb.multi.agent.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * <p><b>EN:</b> Composite primary key for {@link DoctorPatientAssociation} / {@code doctor_patients}.</p>
 * <p><b>PT-BR:</b> Chave composta para {@link DoctorPatientAssociation} / {@code doctor_patients}.</p>
 */
@Embeddable
public class DoctorPatientKey implements Serializable {

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(name = "patient_user_id", nullable = false)
	private UUID patientUserId;

	protected DoctorPatientKey() {
	}

	public DoctorPatientKey(UUID doctorId, UUID patientUserId) {
		this.doctorId = Objects.requireNonNull(doctorId, "doctorId");
		this.patientUserId = Objects.requireNonNull(patientUserId, "patientUserId");
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public UUID getPatientUserId() {
		return patientUserId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DoctorPatientKey other)) {
			return false;
		}
		return Objects.equals(doctorId, other.doctorId) && Objects.equals(patientUserId, other.patientUserId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(doctorId, patientUserId);
	}
}
