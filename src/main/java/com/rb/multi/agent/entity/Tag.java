package com.rb.multi.agent.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.rb.multi.agent.constants.TagCategory;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * <p><b>EN:</b> Global tag catalogue — SAD §7.3 (<code>tags</code> table). Each tag may belong to more than one
 * {@link TagCategory} (join table <code>tag_categories</code>).</p>
 * <p><b>PT-BR:</b> Catálogo global de tags; uma tag pode ter várias categorias.</p>
 */
@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(name = "uk_tags_name", columnNames = "name"))
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/**
	 * EN: Immutable stable key for integrations and i18n ({@code tag.catalog.{code}.name}). PT-BR: Chave estável para i18n.
	 */
	@Column(name = "code", length = 64, nullable = false, unique = true)
	private String code;

	@Column(name = "name", length = 50, nullable = false)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "tag_categories", joinColumns = @JoinColumn(name = "tag_id", nullable = false))
	@Column(name = "category", length = 50, nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<TagCategory> categories = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** EN: JPA-only. PT-BR: Apenas JPA. */
	protected Tag() {
	}

	/**
	 * <p><b>EN:</b> Validates {@code code}, {@code name}, and non-empty {@code categories}.</p>
	 * <p><b>PT-BR:</b> Valida {@code code}, {@code name} e categorias não vazias.</p>
	 */
	public Tag(String code, String name, String description, Collection<TagCategory> categories) {
		this.code = validateAndNormalizeCode(code);
		this.name = Objects.requireNonNull(name, "name").trim();
		if (this.name.isEmpty()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (this.name.length() > 50) {
			throw new IllegalArgumentException("name must be at most 50 characters");
		}
		this.description = description;
		Objects.requireNonNull(categories, "categories");
		if (categories.isEmpty()) {
			throw new IllegalArgumentException("categories must not be empty");
		}
		this.categories = new HashSet<>(categories);
	}

	/** EN: Max length of {@link #code}. PT-BR: Comprimento máximo do {@code code}. */
	public static final int MAX_CODE_LEN = 64;

	/** EN: Normalizes request input to the same canonical form stored in {@link #getCode()}. */
	public static String normalizeCatalogCode(String code) {
		return validateAndNormalizeCode(code);
	}

	private static String validateAndNormalizeCode(String code) {
		String c = Objects.requireNonNull(code, "code").trim().toUpperCase(Locale.ROOT);
		if (c.isEmpty()) {
			throw new IllegalArgumentException("code must not be blank");
		}
		if (c.length() > MAX_CODE_LEN) {
			throw new IllegalArgumentException("code must be at most " + MAX_CODE_LEN + " characters");
		}
		for (int i = 0; i < c.length(); i++) {
			char ch = c.charAt(i);
			boolean ok = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_';
			if (!ok) {
				throw new IllegalArgumentException("code must use A–Z, 0–9 or underscore, got: " + c);
			}
		}
		return c;
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

	public String getCode() {
		return code;
	}

	private void setCode(String code) {
		this.code = code;
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

	public Set<TagCategory> getCategories() {
		return Collections.unmodifiableSet(categories);
	}

	public void setCategories(Collection<TagCategory> categories) {
		Objects.requireNonNull(categories, "categories");
		if (categories.isEmpty()) {
			throw new IllegalArgumentException("categories must not be empty");
		}
		this.categories = new HashSet<>(categories);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
