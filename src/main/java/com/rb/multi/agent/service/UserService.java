package com.rb.multi.agent.service;

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

import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
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

	public UserService(UserRepository userRepository, TagRepository tagRepository) {
		this.userRepository = userRepository;
		this.tagRepository = tagRepository;
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

	/** EN: Inserts enforcing normalized unique {@code code}. PT-BR: Inserção com {@code code} único normalizado. */
	@Transactional
	public User create(UserWriteRequest request) {
		String normalizedCode = normalizeCode(request.code());
		if (userRepository.existsByCode(normalizedCode)) {
			throw new DuplicateUserCodeException(normalizedCode);
		}
		User entity = new User(normalizedCode, request.doctor());
		applyProfile(entity, request);
		syncTags(entity, request);
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
		applyProfile(entity, request);
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

	/** EN: Copies demographics from DTO respecting nullability semantics. PT-BR: Copia dados demográficos do DTO com semântica de nulos esperada. */
	private static void applyProfile(User entity, UserWriteRequest request) {
		entity.setAge(request.age());
		entity.setProfession(request.profession());
		entity.setPostalCode(request.postalCode());
		entity.setCountry(request.country());
		entity.setCity(request.city());
		entity.setAddressLine(request.addressLine());
	}

	/**
	 * <p><b>EN:</b> Replaces join-table rows wholesale from {@link UserWriteRequest#tagIds()}, preserving first-seen UUID order.</p>
	 * <p><b>PT-BR:</b> Substitui linhas da junção segundo {@link UserWriteRequest#tagIds()}, mantendo a ordem de primeiro aparecimento.</p>
	 */
	private void syncTags(User entity, UserWriteRequest request) {
		List<UUID> incoming = request.tagIds();
		if (incoming.isEmpty()) {
			entity.getTags().clear();
			return;
		}
		Set<UUID> uniqueOrdered = incoming.stream().collect(Collectors.toCollection(LinkedHashSet::new));
		List<Tag> resolved = tagRepository.findAllById(uniqueOrdered);
		if (resolved.size() != uniqueOrdered.size()) {
			Set<UUID> foundIds = resolved.stream().map(Tag::getId).collect(Collectors.toSet());
			Set<UUID> missing =
					uniqueOrdered.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toCollection(LinkedHashSet::new));
			throw new UnknownTagReferencesException(missing);
		}
		Map<UUID, Tag> byId = resolved.stream().collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> a));
		entity.getTags().clear();
		for (UUID id : uniqueOrdered) {
			entity.getTags().add(Objects.requireNonNull(byId.get(id)));
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
