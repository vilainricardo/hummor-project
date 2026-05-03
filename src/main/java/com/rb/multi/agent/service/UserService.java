package com.rb.multi.agent.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.rb.multi.agent.dto.UserCreateRequest;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.entity.UserTagAssignment;
import com.rb.multi.agent.exception.AssigningActorNotDoctorException;
import com.rb.multi.agent.exception.AssigningDoctorNotFoundException;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.TagAssignmentDoctorRequiredException;
import com.rb.multi.agent.exception.TagAssignmentPatientOnlyException;
import com.rb.multi.agent.exception.UnknownTagReferencesException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * <p><b>EN:</b> Application service coordinating {@link User} lifecycle rules (code uniqueness + profile mapping).</p>
 * <p><b>PT-BR:</b> Serviço de aplicação com regras de ciclo de vida de {@link User} (unicidade do code + mapeamento de perfil).</p>
 */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final TagRepository tagRepository;
	private final EntityManager entityManager;

	public UserService(UserRepository userRepository, TagRepository tagRepository, EntityManager entityManager) {
		this.userRepository = userRepository;
		this.tagRepository = tagRepository;
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

	/** EN: Inserts enforcing normalized unique {@code code}; tag associations start empty until update. PT-BR: Inserção com {@code code} único; tags apenas após actualização. */
	@Transactional
	public User create(UserCreateRequest request) {
		String normalizedCode = normalizeCode(request.code());
		if (userRepository.existsByCode(normalizedCode)) {
			throw new DuplicateUserCodeException(normalizedCode);
		}
		User entity = new User(normalizedCode, request.doctor());
		applyProfile(entity, request.age(), request.profession(), request.postalCode(), request.country(), request.city(),
				request.addressLine());
		return userRepository.save(entity);
	}

	/** EN: Applies write-request allowing code swaps when uniqueness holds. PT-BR: Actualiza inclusivé troca de code se unicidade for mantida. */
	@Transactional
	public User update(UUID id, UserWriteRequest request) {
		User entity = userRepository.findWithTagsById(id).orElseThrow(() -> UserNotFoundException.byId(id));

		String normalizedCode = normalizeCode(request.code());
		userRepository.findByCode(normalizedCode)
				.filter(other -> !other.getId().equals(entity.getId()))
				.ifPresent(other -> {
					throw new DuplicateUserCodeException(normalizedCode);
				});

		entity.setCode(normalizedCode);
		entity.setDoctor(request.doctor());
		applyProfile(entity, request.age(), request.profession(), request.postalCode(), request.country(), request.city(),
				request.addressLine());
		syncTags(entity, request);
		return userRepository.save(entity);
	}

	/** EN: Removes aggregate when present otherwise {@link UserNotFoundException}. PT-BR: Remove agregado; senão lança {@link UserNotFoundException}. */
	@Transactional
	public void deleteById(UUID id) {
		if (!userRepository.existsById(id)) {
			throw UserNotFoundException.byId(id);
		}
		userRepository.deleteById(id);
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

	/**
	 * <p><b>EN:</b> Replaces clinician assignments when {@link UserWriteRequest#tagIds()} differs from persisted state;
	 * preserves first-seen UUID order; assigns at most five tags per patient; requires acting doctor identity.</p>
	 * <p><b>PT-BR:</b> Substitui atribuições do clínico quando {@code tagIds} difere do estado; até cinco etiquetas por
	 * paciente; exige médico responsável quando há alteração.</p>
	 */
	private void syncTags(User entity, UserWriteRequest request) {
		List<UUID> incoming = request.tagIds();
		LinkedHashSet<UUID> uniqueOrdered = incoming.stream().collect(Collectors.toCollection(LinkedHashSet::new));
		Set<UUID> currentIds =
				entity.getTagAssignments().stream().map(a -> a.getTag().getId()).collect(Collectors.toSet());
		if (currentIds.equals(uniqueOrdered)) {
			return;
		}

		UUID assigningDoctorId = request.assignedByDoctorId();
		if (assigningDoctorId == null) {
			throw new TagAssignmentDoctorRequiredException();
		}
		if (entity.isDoctor()) {
			throw new TagAssignmentPatientOnlyException(entity.getCode());
		}

		User assigningDoctor =
				userRepository.findById(assigningDoctorId).orElseThrow(() -> new AssigningDoctorNotFoundException(assigningDoctorId));
		if (!assigningDoctor.isDoctor()) {
			throw new AssigningActorNotDoctorException(assigningDoctor.getCode());
		}

		Map<UUID, Tag> byId;
		if (uniqueOrdered.isEmpty()) {
			byId = Map.of();
		} else {
			List<Tag> resolved = tagRepository.findAllById(uniqueOrdered);
			if (resolved.size() != uniqueOrdered.size()) {
				Set<UUID> foundIds = resolved.stream().map(Tag::getId).collect(Collectors.toSet());
				Set<UUID> missing =
						uniqueOrdered.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toCollection(LinkedHashSet::new));
				throw new UnknownTagReferencesException(missing);
			}
			byId = resolved.stream().collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> a));
		}

		entity.getTagAssignments().clear();
		entityManager.flush();
		Instant assignedAt = Instant.now();
		for (UUID id : uniqueOrdered) {
			new UserTagAssignment(entity, Objects.requireNonNull(byId.get(id)), assigningDoctor, assignedAt);
		}
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
}
