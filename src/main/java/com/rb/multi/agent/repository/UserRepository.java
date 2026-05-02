package com.rb.multi.agent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.User;

/**
 * <p><b>EN:</b> Persistence accessor for {@link User} keyed by surrogate UUID plus unique {@code code} lookup.</p>
 * <p><b>PT-BR:</b> Acesso a {@link User} por UUID surrogate mais consultas pelo {@code code} único.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByCode(String code);

	boolean existsByCode(String code);
}
