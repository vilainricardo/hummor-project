package com.rb.multi.agent.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Todo utilizador registado é sempre um <strong>paciente</strong>; pode também ter função médica via {@link #isDoctor()}.
 *
 * Coluna persistida: {@code is_doctor}. Alinhado ao SAD §7.2 (<code>users</code>).
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

	protected User() {
	}

	public User(String code) {
		this(code, false);
	}

	public User(String code, boolean isDoctor) {
		this.code = Objects.requireNonNull(code, "code");
		this.isDoctor = isDoctor;
	}

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
