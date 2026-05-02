package com.rb.multi.agent.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rb.multi.agent.dto.TagResponse;
import com.rb.multi.agent.dto.TagWriteRequest;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.exception.TagNotFoundException;
import com.rb.multi.agent.service.TagService;

import jakarta.validation.Valid;

/**
 * <p><b>EN:</b> CRUD catalogue for tags under {@code /api/v1/tags} (SAD §7.3).</p>
 * <p><b>PT-BR:</b> CRUD do catálogo de tags em {@code /api/v1/tags} (SAD §7.3).</p>
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

	private final TagService tagService;

	public TagController(TagService tagService) {
		this.tagService = tagService;
	}

	/**
	 * <p><b>EN:</b> Lists tags, optionally narrowed by {@link TagCategory} to avoid fetching the entire catalogue.</p>
	 * <p><b>PT-BR:</b> Lista tags, opcionalmente filtradas por {@link TagCategory} para não carregar o catálogo inteiro.</p>
	 */
	@GetMapping
	public List<TagResponse> list(@RequestParam(required = false) TagCategory category) {
		return tagService.findAll(category).stream().map(TagResponse::from).toList();
	}

	/** EN: Loads tag primary key. PT-BR: Obtém etiqueta pela chave (UUID). */
	@GetMapping("/{id}")
	public TagResponse getById(@PathVariable UUID id) {
		return tagService.findById(id).map(TagResponse::from).orElseThrow(() -> TagNotFoundException.byId(id));
	}

	/** EN: Resolves immutable tag slug by canonical name. PT-BR: Resolve etiqueta pelo nome canónico (case-insensitive). */
	@GetMapping("/by-name/{name}")
	public TagResponse getByName(@PathVariable String name) {
		return tagService.findByNameIgnoreCase(name)
				.map(TagResponse::from)
				.orElseThrow(() -> TagNotFoundException.byName(name));
	}

	/** EN: Creates tag returning {@code Location} header. PT-BR: Cria tag com cabeçalho {@code Location}. */
	@PostMapping
	public ResponseEntity<TagResponse> create(@Valid @RequestBody TagWriteRequest request) {
		var saved = tagService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(saved.getId())
				.toUri();
		return ResponseEntity.created(location).body(TagResponse.from(saved));
	}

	/** EN: Idempotent replace of editable fields. PT-BR: Substituição idempotente dos campos editáveis. */
	@PutMapping("/{id}")
	public TagResponse update(@PathVariable UUID id, @Valid @RequestBody TagWriteRequest request) {
		return TagResponse.from(tagService.update(id, request));
	}

	/** EN: Hard delete catalogue row when present. PT-BR: Elimina linha do catálogo quando existente. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		tagService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
