package com.rb.multi.agent.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * <p><b>EN:</b> Global tag catalogue — SAD §7.3 (<code>tags</code> table).</p>
 * <p><b>PT-BR:</b> Catálogo global de tags — SAD §7.3 (tabela <code>tags</code>).</p>
 */
@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(name = "uk_tags_name", columnNames = "name"))
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "name", length = 50, nullable = false)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", length = 50, nullable = false)
	private TagCategory category;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** EN: JPA-only. PT-BR: Apenas JPA. */
	protected Tag() {
	}

	/**
	 * <p><b>EN:</b> Validates persisted {@code name} length and trims leading/trailing spaces.</p>
	 * <p><b>PT-BR:</b> Valida comprimento persistido de {@code name} removendo espaços à volta.</p>
	 */
	public Tag(String name, String description, TagCategory category) {
		this.name = Objects.requireNonNull(name, "name").trim();
		if (this.name.isEmpty()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (this.name.length() > 50) {
			throw new IllegalArgumentException("name must be at most 50 characters");
		}
		this.description = description;
		this.category = Objects.requireNonNull(category, "category");
	}

	/**
	 * <p><b>EN:</b> Ensures {@code created_at} exists before first insert.</p>
	 * <p><b>PT-BR:</b> Garante {@code created_at} antes da primeira persistência.</p>
	 */
	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name, "name").trim();
		if (this.name.isEmpty()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (this.name.length() > 50) {
			throw new IllegalArgumentException("name must be at most 50 characters");
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TagCategory getCategory() {
		return category;
	}

	public void setCategory(TagCategory category) {
		this.category = Objects.requireNonNull(category, "category");
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
