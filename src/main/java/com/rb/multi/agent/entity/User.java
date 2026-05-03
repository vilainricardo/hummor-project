package com.rb.multi.agent.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * <p><b>EN:</b> Every account is a patient first; clinician capabilities use {@link #isDoctor()}; column {@code is_doctor}.
 * SAD §7.2 (<code>users</code>). Optional patient catalogue tags via {@link #getTags()} / <code>user_tag_assignments</code>.</p>
 * <p><b>PT-BR:</b> Toda conta é paciente primeiro; função clínica via {@link #isDoctor()}; coluna {@code is_doctor}.
 * SAD §7.2 (<code>users</code>). Etiquetas de catálogo para pacientes via {@link #getTags()} / <code>user_tag_assignments</code>.</p>
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "code", length = 20, nullable = false, unique = true)
	private String code;

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
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		User user = (User) o;
		return id != null && id.equals(user.id);
	}

	@Override
	public int hashCode() {
		return id == null ? super.hashCode() : id.hashCode();
	}
}
