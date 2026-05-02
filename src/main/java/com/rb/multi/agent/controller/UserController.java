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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rb.multi.agent.dto.UserResponse;
import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.service.UserService;

import jakarta.validation.Valid;

/**
 * <p><b>EN:</b> CRUD facade for persisted {@link com.rb.multi.agent.entity.User} aggregates (SAD §7.2).</p>
 * <p><b>PT-BR:</b> Fachada CRUD sobre agregados {@link com.rb.multi.agent.entity.User} persistidos (SAD §7.2).</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/** EN: Full user list projection. PT-BR: Lista todos os utilizadores (projeção de leitura). */
	@GetMapping
	public List<UserResponse> list() {
		return userService.findAll().stream().map(UserResponse::from).toList();
	}

	/** EN: Loads user by surrogate UUID key. PT-BR: Obtém utilizador pela chave UUID. */
	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable UUID id) {
		return userService.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> UserNotFoundException.byId(id));
	}

	/** EN: Finds user via unique public-facing {@code code}. PT-BR: Encontra utilizador pelo {@code code} público único. */
	@GetMapping("/by-code/{code}")
	public UserResponse getByCode(@PathVariable String code) {
		return userService.findByCode(code)
				.map(UserResponse::from)
				.orElseThrow(() -> UserNotFoundException.byCode(code));
	}

	/** EN: Registers profile + {@code Location}. PT-BR: Regista perfil devolvendo cabeçalho {@code Location}. */
	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody UserWriteRequest request) {
		var saved = userService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(saved.getId())
				.toUri();
		return ResponseEntity.created(location).body(UserResponse.from(saved));
	}

	/** EN: PATCH-style replace constrained by uniqueness rules. PT-BR: Actualização tipo replace respeitando unicidade. */
	@PutMapping("/{id}")
	public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserWriteRequest request) {
		var saved = userService.update(id, request);
		return UserResponse.from(saved);
	}

	/** EN: Deletes when row exists — 404 via service otherwise. PT-BR: Remove se existir; serviço gera 404 se não existir. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		userService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
