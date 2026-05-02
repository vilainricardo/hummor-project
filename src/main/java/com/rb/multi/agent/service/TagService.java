package com.rb.multi.agent.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.TagWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.exception.DuplicateTagNameException;
import com.rb.multi.agent.exception.TagNotFoundException;
import com.rb.multi.agent.repository.TagRepository;

/**
 * <p><b>EN:</b> Thin orchestration enforcing catalogue invariants atop {@link TagRepository}.</p>
 * <p><b>PT-BR:</b> Orquestração fina aplicando invariantes do catálogo sobre {@link TagRepository}.</p>
 */
@Service
public class TagService {

	private final TagRepository tagRepository;

	public TagService(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	/** EN: Returns all rows or category slice. PT-BR: Devolve todas as linhas ou fatia por categoria. */
	@Transactional(readOnly = true)
	public List<Tag> findAll(TagCategory categoryFilter) {
		if (categoryFilter == null) {
			return tagRepository.findAll();
		}
		return tagRepository.findAllByCategory(categoryFilter);
	}

	/** EN: Optional fetch by surrogate id. PT-BR: Busca opcional por id surrogate. */
	@Transactional(readOnly = true)
	public Optional<Tag> findById(UUID id) {
		return tagRepository.findById(id);
	}

	/** EN: Case-insensitive name lookup skipping blank needles. PT-BR: Pesquisa por nome insensível a maiúsculas; branco → vazio. */
	@Transactional(readOnly = true)
	public Optional<Tag> findByNameIgnoreCase(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		return tagRepository.findByNameIgnoreCase(normalizeName(name));
	}

	/** EN: Persist new catalogue entry after duplicate check. PT-BR: Persiste entrada nova após verificação de duplicado. */
	@Transactional
	public Tag create(TagWriteRequest request) {
		String name = normalizeName(request.name());
		if (tagRepository.existsByNameIgnoreCase(name)) {
			throw new DuplicateTagNameException(name);
		}
		Objects.requireNonNull(request.category(), "category");
		Tag tag = new Tag(name, trimToNull(request.description()), request.category());
		return tagRepository.save(tag);
	}

	/** EN: Applies write model while guarding global name uniqueness. PT-BR: Aplica modelo de escrita garantindo unicidade global do nome. */
	@Transactional
	public Tag update(UUID id, TagWriteRequest request) {
		Tag entity = tagRepository.findById(id).orElseThrow(() -> TagNotFoundException.byId(id));

		String name = normalizeName(request.name());
		tagRepository.findByNameIgnoreCase(name)
				.filter(other -> !other.getId().equals(entity.getId()))
				.ifPresent(other -> {
					throw new DuplicateTagNameException(name);
				});

		entity.setName(name);
		entity.setDescription(trimToNull(request.description()));
		entity.setCategory(Objects.requireNonNull(request.category(), "category"));
		return tagRepository.save(entity);
	}

	/** EN: Deletes catalogue row ensuring idempotent 404 signalling. PT-BR: Remove linha existente; caso contrário sinalização 404. */
	@Transactional
	public void deleteById(UUID id) {
		if (!tagRepository.existsById(id)) {
			throw TagNotFoundException.byId(id);
		}
		tagRepository.deleteById(id);
	}

	/** EN: Trims/strips enforced length-bound tag identifier. PT-BR: Normaliza nome com strip e limite de tamanho aplicado no domínio. */
	private static String normalizeName(String name) {
		String stripped = Objects.requireNonNull(name, "name").strip();
		if (stripped.isEmpty()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (stripped.length() > 50) {
			throw new IllegalArgumentException("name must be at most 50 characters");
		}
		return stripped;
	}

	/** EN: Blank-ish strings collapsed to {@code null} for persistence. PT-BR: Strings em branco colapsadas em {@code null} para BD. */
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String t = value.strip();
		return t.isEmpty() ? null : t;
	}
}
