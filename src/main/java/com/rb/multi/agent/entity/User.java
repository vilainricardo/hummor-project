package com.rb.multi.agent.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> Every account is a patient first; clinician capabilities use {@link #isDoctor()}; column {@code is_doctor}.
 * SAD §7.2 (<code>users</code>). {@link Doctor} is the JOINED subclass (table {@code doctors}). Optional patient catalogue
 * tags via {@link #getTags()} / <code>user_tag_assignments</code>.</p>
 * <p><b>PT-BR:</b> Toda conta é paciente primeiro; perfil médico como subclasse {@link Doctor}.
 * Etiquetas de catálogo via {@link #getTags()} / <code>user_tag_assignments</code>.</p>
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "code", length = 20, nullable = false, unique = true)
	private String code;

	/** EN: Normalized unique inbox (trim + lower case); optional only for legacy rows pre-migration. PT-BR: E-mail único normalizado. */
	@Column(name = "email", length = 320, unique = true)
	private String email;

	/** EN: Argon2id-encoded credential; {@code null} for OAuth-only or unset. PT-BR: Credencial codificada Argon2id; {@code null} se só OAuth ou não definido. */
	@Column(name = "password_hash", columnDefinition = "TEXT")
	private String passwordHash;

	@Column(name = "is_doctor", nullable = false)
	private boolean isDoctor;

	@Column(name = "age")
	private Integer age;

	@Column(name = "profession", length = 100)
	private String profession;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(name = "country", length = 100)
	private String country;

	@Column(name = "city", length = 100)
	private String city;

	@Column(name = "address_line", columnDefinition = "TEXT")
	private String addressLine;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** EN: Patient-to-tag memberships with clinician provenance (lazy). PT-BR: Ligações paciente–tag com registo do clínico (lazy). */
	@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<UserTagAssignment> tagAssignments = new LinkedHashSet<>();

	/** EN: JPA-only. PT-BR: Apenas JPA. */
	protected User() {
	}

	/**
	 * <p><b>EN:</b> Patient-only convenience ctor — {@code doctor} defaults {@code false}.</p>
	 * <p><b>PT-BR:</b> Atalho só paciente — {@code doctor} fica {@code false}.</p>
	 */
	public User(String code) {
		this(code, false);
	}

	/**
	 * <p><b>EN:</b> Persists clinician flag aligned with SRS patient-first wording.</p>
	 * <p><b>PT-BR:</b> Persistência do flag médico alinhado com modelo SRS “paciente primeiro”.</p>
	 */
	public User(String code, boolean isDoctor) {
		this.code = Objects.requireNonNull(code, "code");
		this.isDoctor = isDoctor;
	}

	/**
	 * <p><b>EN:</b> Ensures {@code created_at} exists before first insert.</p>
	 * <p><b>PT-BR:</b> Garante {@code created_at} antes da primeira persistência.</p>
	 */
	@PrePersist
	void populateCreatedAt() {
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

	public void setCode(String code) {
		this.code = code;
	}

	public String getEmail() {
		return email;
	}

	/**
	 * <p><b>EN:</b> Persists canonical form (trim, ASCII lower case). {@code null} clears the column (legacy only).</p>
	 * <p><b>PT-BR:</b> Grava forma canónica (trim, minúsculas). {@code null} limpa a coluna (legado).</p>
	 */
	public void setEmail(String email) {
		if (email == null) {
			this.email = null;
			return;
		}
		String trimmed = email.trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			this.email = null;
			return;
		}
		if (trimmed.length() > 320) {
			throw new IllegalArgumentException("email must be at most 320 characters");
		}
		this.email = trimmed;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/**
	 * <p><b>EN:</b> Integration-test helper: persisted clinician with deterministic unique email derived from {@code code}.</p>
	 * <p><b>PT-BR:</b> Atalho de teste: médico persistível com e-mail único derivado do {@code code}.</p>
	 */
	public static User seedClinician(String publicCode) {
		Doctor u = new Doctor(publicCode);
		u.setEmail(integrationSeedEmail(publicCode));
		return u;
	}

	/**
	 * <p><b>EN:</b> Integration-test helper: persisted patient-shaped row with deterministic email.</p>
	 * <p><b>PT-BR:</b> Atalho de teste: linha tipo paciente com e-mail determinístico.</p>
	 */
	public static User seedPatient(String publicCode) {
		User u = new User(publicCode, false);
		u.setEmail(integrationSeedEmail(publicCode));
		return u;
	}

	/**
	 * <p><b>EN:</b> Deterministic inbox aligned with uniqueness rules (not for production accounts).</p>
	 * <p><b>PT-BR:</b> Inbox determinístico alinhado às regras de unicidade (não usar em produção).</p>
	 */
	public static String integrationSeedEmail(String publicCode) {
		String slug = Objects.requireNonNull(publicCode, "publicCode").trim().toLowerCase(Locale.ROOT);
		return slug + "@seed.integration.invalid";
	}

	public boolean isDoctor() {
		return isDoctor;
	}

	public void setDoctor(boolean isDoctor) {
		this.isDoctor = isDoctor;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getProfession() {
		return profession;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddressLine() {
		return addressLine;
	}

	public void setAddressLine(String addressLine) {
		this.addressLine = addressLine;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	protected void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	/** EN: Mutable set managed by JPA; avoids duplicate refs from join fetch. PT-BR: Set JPA; evita refs duplicadas em fetch joins. */
	public Set<UserTagAssignment> getTagAssignments() {
		return tagAssignments;
	}

	/**
	 * <p><b>EN:</b> Unique catalogue tags from clinician-backed assignments only — read snapshot, not for persistence.</p>
	 * <p><b>PT-BR:</b> Etiquetas únicas do catálogo apenas com linhas atribuídas por médicos — leitura, não usar para persistir.</p>
	 */
	public Set<Tag> getTags() {
		Map<UUID, Tag> unique =
				tagAssignments.stream()
						.filter(a -> a.getAssignedBy() != null && a.getAssignedBy().isDoctor())
						.collect(
								Collectors.toMap(
										a -> a.getTag().getId(),
										UserTagAssignment::getTag,
										(existing, duplicate) -> existing,
										LinkedHashMap::new));
		return new LinkedHashSet<>(unique.values());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof User other)) {
			return false;
		}
		return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id == null ? super.hashCode() : id.hashCode();
	}
}
