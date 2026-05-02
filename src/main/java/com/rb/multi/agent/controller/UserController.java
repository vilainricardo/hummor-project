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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public List<UserResponse> list() {
		return userService.findAll().stream().map(UserResponse::from).toList();
	}

	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable UUID id) {
		return userService.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> UserNotFoundException.byId(id));
	}

	@GetMapping("/by-code/{code}")
	public UserResponse getByCode(@PathVariable String code) {
		return userService.findByCode(code)
				.map(UserResponse::from)
				.orElseThrow(() -> UserNotFoundException.byCode(code));
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody UserWriteRequest request) {
		var saved = userService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(saved.getId())
				.toUri();
		return ResponseEntity.created(location).body(UserResponse.from(saved));
	}

	@PutMapping("/{id}")
	public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserWriteRequest request) {
		var saved = userService.update(id, request);
		return UserResponse.from(saved);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		userService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
