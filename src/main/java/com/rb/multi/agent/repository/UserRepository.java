package com.rb.multi.agent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByCode(String code);

	boolean existsByCode(String code);
}
