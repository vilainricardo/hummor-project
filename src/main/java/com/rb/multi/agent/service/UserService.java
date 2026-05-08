package com.rb.multi.agent.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.rb.multi.agent.dto.UserCreateRequest;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Doctor;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.entity.UserTagAssignment;
import com.rb.multi.agent.exception.AssigningActorNotDoctorException;
import com.rb.multi.agent.exception.AssigningDoctorNotFoundException;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.DuplicateUserEmailException;
import com.rb.multi.agent.exception.PatientTagAssignmentNotFoundException;
import com.rb.multi.agent.exception.TagAssignmentSliceFullException;
import com.rb.multi.agent.exception.UnknownTagReferencesException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.DoctorRepository;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;
import com.rb.multi.agent.security.PasswordHasher;

/**
 * <p><b>EN:</b> Application service coordinating {@link User} lifecycle rules (unique {@code code}, unique {@code email},
 * profile mapping).</p>
 * <p><b>PT-BR:</b> Serviço com regras de {@link User} ({@code code} único, {@code email} único, perfil).</p>
 */
@Service
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MAX_DISTINCT_TAGS_PER_CLINICIAN_PER_PATIENT = 5;

	private final UserRepository userRepository;
	private final TagRepository tagRepository;
	private final PasswordHasher passwordHasher;
	private final DoctorRepository doctorRepository;
	private final EntityManager entityManager;

	public UserService(
			UserRepository userRepository,
			TagRepository tagRepository,
			PasswordHasher passwordHasher,
			DoctorRepository doctorRepository,
			EntityManager entityManager) {
		this.userRepository = userRepository;
		this.tagRepository = tagRepository;
		this.passwordHasher = passwordHasher;
		this.doctorRepository = doctorRepository;
		this.entityManager = entityManager;
	}

	/** EN: Ordered table scan surrogate for admin surfaces. PT-BR: Lista completa adequada para ecrãs administrativos. */
	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userRepository.findAllWithTags();
	}

	/** EN: Locate account by surrogate UUID key. PT-BR: Localiza conta pela chave UUID surrogate. */
	@Transactional(readOnly = true)
	public Optional<User> findById(UUID id) {
		return userRepository.findWithTagsById(id);
	}

	/** EN: Locate account by externally visible deterministic code. PT-BR: Localiza conta pelo {@code code} público determinístico. */
	@Transactional(readOnly = true)
	public Optional<User> findByCode(String code) {
		return userRepository.findWithTagsByCode(code);
	}

	/** EN: Inserts enforcing normalized unique {@code code} and {@code email}; tags start empty until catalogue-tag endpoints. PT-BR: Inserção com {@code code} e e-mail únicos; tags via endpoints dedicados. */
	@Transactional
	public User create(UserCreateRequest request) {
		String normalizedCode = normalizeCode(request.code());
		if (userRepository.existsByCode(normalizedCode)) {
			throw new DuplicateUserCodeException(normalizedCode);
		}
		String normalizedEmail = normalizeEmail(request.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new DuplicateUserEmailException(normalizedEmail);
		}
		User entity = request.doctor() ? new Doctor(normalizedCode) : new User(normalizedCode);
		entity.setEmail(normalizedEmail);
		applyProfile(entity, request.age(), request.profession(), request.postalCode(), request.country(), request.city(),
				request.addressLine());
		applyPasswordHashIfPresent(entity, request.password());
		return userRepository.save(entity);
	}

	/** EN: Applies write-request allowing code swaps when uniqueness holds; ignores tag links here. PT-BR: Actualização de perfil; tags em endpoints separados. */
	@Transactional
	public User update(UUID id, UserWriteRequest request) {
		User entity = userRepository.findWithTagsById(id).orElseThrow(() -> UserNotFoundException.byId(id));

		String normalizedCode = normalizeCode(request.code());
		userRepository.findByCode(normalizedCode)
				.filter(other -> !other.getId().equals(entity.getId()))
				.ifPresent(other -> {
					throw new DuplicateUserCodeException(normalizedCode);
				});

		String normalizedEmail = normalizeEmail(request.email());
		userRepository.findByEmail(normalizedEmail)
				.filter(other -> !other.getId().equals(entity.getId()))
				.ifPresent(other -> {
					throw new DuplicateUserEmailException(normalizedEmail);
				});

		UUID entityId = entity.getId();
		boolean becomingDoctor = request.doctor() && !(entity instanceof Doctor);
		boolean removingDoctor = !request.doctor() && entity instanceof Doctor;
		if (becomingDoctor) {
			doctorRepository.insertDoctorRowIfAbsent(entityId);
			entityManager.flush();
			entityManager.clear();
			entity = userRepository.findWithTagsById(entityId).orElseThrow(() -> UserNotFoundException.byId(entityId));
		}
		if (removingDoctor) {
			doctorRepository.deleteDoctorRow(entityId);
			entityManager.flush();
			entityManager.clear();
			entity = userRepository.findWithTagsById(entityId).orElseThrow(() -> UserNotFoundException.byId(entityId));
		}

		entity.setCode(normalizedCode);
		entity.setEmail(normalizedEmail);
		entity.setDoctor(request.doctor());
		applyProfile(entity, request.age(), request.profession(), request.postalCode(), request.country(), request.city(),
				request.addressLine());
		applyPasswordHashIfPresent(entity, request.password());
		return userRepository.save(entity);
	}

	/**
	 * <p><b>EN:</b> Adds/reaffirms one catalogue-tag slice row; target user may carry {@code isDoctor=true} (still a patient).
	 * Only {@code assignedByDoctorId} must resolve to {@code isDoctor=true}.</p>
	 * <p><b>PT-BR:</b> Acrescenta/reforça uma etiqueta no slice do médico; o utilizador-alvo pode ter {@code isDoctor=true}
	 * (é paciente com capacidade médica). Só o {@code assignedByDoctorId} tem de ser médico efectivo.</p>
	 */
	@Transactional
	public User assignCatalogueTag(UUID patientId, UUID assignedByDoctorId, UUID tagId) {
		User patient = userRepository.findWithTagsById(patientId).orElseThrow(() -> UserNotFoundException.byId(patientId));
		User clinician = resolveAssigningDoctor(assignedByDoctorId);

		boolean alreadyHeld =
				patient.getTagAssignments().stream().anyMatch(
						a -> assignedByDoctorId.equals(a.getAssignedBy().getId()) && tagId.equals(a.getTag().getId()));
		if (alreadyHeld) {
			return userRepository.save(patient);
		}

		Tag tag =
				tagRepository.findById(tagId).orElseThrow(() -> new UnknownTagReferencesException(Set.of(tagId)));

		long distinctHeldByClinician =
				patient.getTagAssignments().stream()
						.filter(a -> assignedByDoctorId.equals(a.getAssignedBy().getId()))
						.map(a -> a.getTag().getId())
						.distinct()
						.count();
		if (distinctHeldByClinician >= MAX_DISTINCT_TAGS_PER_CLINICIAN_PER_PATIENT) {
			throw new TagAssignmentSliceFullException(patientId, assignedByDoctorId);
		}

		new UserTagAssignment(patient, tag, clinician, Instant.now());
		return userRepository.save(patient);
	}

	/**
	 * <p><b>EN:</b> Drops one clinician slice assignment for the patient catalogue tag triple.</p>
	 * <p><b>PT-BR:</b> Remove uma atribuição do slice do médico para essa etiqueta.</p>
	 */
	@Transactional
	public User removeCatalogueTag(UUID patientId, UUID assignedByDoctorId, UUID tagId) {
		User patient = userRepository.findWithTagsById(patientId).orElseThrow(() -> UserNotFoundException.byId(patientId));
		boolean removed =
				patient.getTagAssignments().removeIf(
						a ->
								assignedByDoctorId.equals(a.getAssignedBy().getId())
										&& tagId.equals(a.getTag().getId()));
		if (!removed) {
			throw new PatientTagAssignmentNotFoundException(patientId, assignedByDoctorId, tagId);
		}
		return userRepository.save(patient);
	}

	private User resolveAssigningDoctor(UUID assignedByDoctorId) {
		User assigner =
				userRepository.findById(assignedByDoctorId).orElseThrow(() -> new AssigningDoctorNotFoundException(assignedByDoctorId));
		if (!assigner.isDoctor()) {
			throw new AssigningActorNotDoctorException(assigner.getCode());
		}
		return assigner;
	}

	/** EN: Removes aggregate when present otherwise {@link UserNotFoundException}. PT-BR: Remove agregado; senão lança {@link UserNotFoundException}. */
	@Transactional
	public void deleteById(UUID id) {
		if (!userRepository.existsById(id)) {
			throw UserNotFoundException.byId(id);
		}
		userRepository.deleteById(id);
	}

	/** EN: Non-blank password (after trim) replaces {@code password_hash}; {@code null} or blank leaves column unchanged. PT-BR: Palavra-passe não vazia (após trim) substitui {@code password_hash}; {@code null} ou vazio mantém o valor. */
	private void applyPasswordHashIfPresent(User entity, String rawPassword) {
		String normalized = normalizePasswordOrNull(rawPassword);
		if (normalized == null) {
			return;
		}
		if (normalized.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
		entity.setPasswordHash(passwordHasher.hash(normalized));
	}

	private static String normalizePasswordOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** EN: Copies demographics respecting nullability semantics. PT-BR: Copia dados demográficos com semântica de nulos esperada. */
	private static void applyProfile(
			User entity,
			Integer age,
			String profession,
			String postalCode,
			String country,
			String city,
			String addressLine) {
		entity.setAge(age);
		entity.setProfession(profession);
		entity.setPostalCode(postalCode);
		entity.setCountry(country);
		entity.setCity(city);
		entity.setAddressLine(addressLine);
	}

	/** EN: Validates trimmed length-bounded canonical user code text. PT-BR: Valida texto do code utilizador com strip e comprimento máximo. */
	private static String normalizeCode(String code) {
		String trimmed = Objects.requireNonNull(code, "code").trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("code must not be blank");
		}
		if (trimmed.length() > 20) {
			throw new IllegalArgumentException("code must be at most 20 characters");
		}
		return trimmed;
	}

	/** EN: Trim + lowercase; matches persisted {@link User#setEmail}; Bean Validation catches most blanks. PT-BR: Normalização alinhada a {@link User#setEmail}. */
	private static String normalizeEmail(String raw) {
		String trimmed = Objects.requireNonNull(raw, "email").trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("email must not be blank");
		}
		if (trimmed.length() > 320) {
			throw new IllegalArgumentException("email must be at most 320 characters");
		}
		return trimmed;
	}
}
