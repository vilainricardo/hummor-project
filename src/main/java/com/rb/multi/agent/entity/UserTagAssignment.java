package com.rb.multi.agent.entity;

import java.time.Instant;
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
 * <p><b>EN:</b> Join rows for patient-tag links created by clinicians; replaces bare {@code user_tags} with audit fields.</p>
 * <p><b>PT-BR:</b> Linhas da junção paciente–tag criadas pelo clínico; substitui {@code user_tags} simples com metadados.</p>
 */
@Entity
@Table(
		name = "user_tag_assignments",
		uniqueConstraints = @UniqueConstraint(name = "uk_uta_patient_tag", columnNames = {"patient_id", "tag_id"}))
public class UserTagAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private User patient;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tag_id", nullable = false)
	private Tag tag;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assigned_by_user_id", nullable = false)
	private User assignedBy;

	@Column(name = "assigned_at", nullable = false)
	private Instant assignedAt;

	protected UserTagAssignment() {
	}

	public UserTagAssignment(User patient, Tag tag, User assignedBy, Instant assignedAt) {
		this.patient = Objects.requireNonNull(patient, "patient");
		this.tag = Objects.requireNonNull(tag, "tag");
		this.assignedBy = Objects.requireNonNull(assignedBy, "assignedBy");
		this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
		patient.getTagAssignments().add(this);
	}

	public UUID getId() {
		return id;
	}

	public User getPatient() {
		return patient;
	}

	public Tag getTag() {
		return tag;
	}

	public User getAssignedBy() {
		return assignedBy;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}

	private UUID patientIdOrNull() {
		return patient == null ? null : patient.getId();
	}

	private UUID tagIdOrNull() {
		return tag == null ? null : tag.getId();
	}

	/**
	 * <p><b>EN:</b> Stable business key ({@code patient} + {@code tag}) so {@link User} can use a persistent {@link java.util.Set}.</p>
	 * <p><b>PT-BR:</b> Chave estável paciente+tag para o {@link User} usar um {@link java.util.Set} persistido pelo JPA.</p>
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UserTagAssignment that = (UserTagAssignment) o;
		return Objects.equals(patientIdOrNull(), that.patientIdOrNull()) && Objects.equals(tagIdOrNull(), that.tagIdOrNull());
	}

	@Override
	public int hashCode() {
		return Objects.hash(patientIdOrNull(), tagIdOrNull());
	}
}
