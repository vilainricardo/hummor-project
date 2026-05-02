package com.rb.multi.agent.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.UserWriteRequest;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.DuplicateUserCodeException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<User> findById(UUID id) {
		return userRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByCode(String code) {
		return userRepository.findByCode(code);
	}

	@Transactional
	public User create(UserWriteRequest request) {
		String normalizedCode = normalizeCode(request.code());
		if (userRepository.existsByCode(normalizedCode)) {
			throw new DuplicateUserCodeException(normalizedCode);
		}
		User entity = new User(normalizedCode, request.doctor());
		applyProfile(entity, request);
		return userRepository.save(entity);
	}

	@Transactional
	public User update(UUID id, UserWriteRequest request) {
		User entity = userRepository.findById(id).orElseThrow(() -> UserNotFoundException.byId(id));

		String normalizedCode = normalizeCode(request.code());
		userRepository.findByCode(normalizedCode)
				.filter(other -> !other.getId().equals(entity.getId()))
				.ifPresent(other -> {
					throw new DuplicateUserCodeException(normalizedCode);
				});

		entity.setCode(normalizedCode);
		entity.setDoctor(request.doctor());
		applyProfile(entity, request);
		return userRepository.save(entity);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!userRepository.existsById(id)) {
			throw UserNotFoundException.byId(id);
		}
		userRepository.deleteById(id);
	}

	private static void applyProfile(User entity, UserWriteRequest request) {
		entity.setAge(request.age());
		entity.setProfession(request.profession());
		entity.setPostalCode(request.postalCode());
		entity.setCountry(request.country());
		entity.setCity(request.city());
		entity.setAddressLine(request.addressLine());
	}

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
