package com.rb.multi.agent.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> One patient-submitted sleep rating (SRS <strong>FR-006</strong> — mood is FR-005). Append-style row; score is an
 * inclusive 0–10 self-assessment axis (product scale, not clinical scoring).</p>
 * <p><b>PT-BR:</b> Um registo de sono do paciente (SRS <strong>FR-006</strong>; humor é FR-005). Linha append-only; valor 0–10
 * inclusive (escala de produto).</p>
 */
@Entity
@Table(name = "sleep_entries")
@Check(constraints = "value >= 0 AND value <= 10")
public class SleepEntry {

	/** EN: Minimum inclusive score. PT-BR: Mínimo inclusivo. */
	public static final int MIN_SCORE = 0;

	/** EN: Maximum inclusive score. PT-BR: Máximo inclusivo. */
	public static final int MAX_SCORE = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", nullable = false)
	private User patient;

	@Column(name = "value", nullable = false)
	private int value;

	/** EN: Calendar day (UTC) this score refers to — at most one row per patient per day. PT-BR: Dia civil (UTC) do registo. */
	@Column(name = "recorded_on", nullable = false, columnDefinition = "DATE")
	private LocalDate recordedOn;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected SleepEntry() {
	}

	/**
	 * EN: New sleep entry for {@code recordedOn} (UTC calendar day); {@code value} ∈ [0, 10]; {@link #createdAt} at persist if unset.
	 * PT-BR: Novo registo de sono para o dia {@code recordedOn}; {@code created_at} na persistência se não definido.
	 */
	public SleepEntry(User patient, int value, LocalDate recordedOn) {
		this.patient = Objects.requireNonNull(patient, "patient");
		this.value = checkScore(value);
		this.recordedOn = Objects.requireNonNull(recordedOn, "recordedOn");
	}

	private static int checkScore(int value) {
		if (value < MIN_SCORE || value > MAX_SCORE) {
			throw new IllegalArgumentException(
					"sleep value must be between " + MIN_SCORE + " and " + MAX_SCORE + " inclusive, got " + value);
		}
		return value;
	}

	@PrePersist
	void stampCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public User getPatient() {
		return patient;
	}

	public int getValue() {
		return value;
	}

	public LocalDate getRecordedOn() {
		return recordedOn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
