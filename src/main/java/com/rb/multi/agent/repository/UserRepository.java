package com.rb.multi.agent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rb.multi.agent.entity.User;

/**
 * <p><b>EN:</b> Persistence accessor for {@link User} keyed by surrogate UUID plus unique {@code code} lookup.</p>
 * <p><b>PT-BR:</b> Acesso a {@link User} por UUID surrogate mais consultas pelo {@code code} único.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByCode(String code);

	boolean existsByCode(String code);

	/** EN: Hydrates catalogue tags eagerly to avoid cartesian explosions in listings. PT-BR: Carrega tags com fetch join nas listagens. */
	@Query(
			"select distinct u from User u left join fetch u.tagAssignments ua left join fetch ua.tag "
					+ "left join fetch ua.assignedBy")
	List<User> findAllWithTags();

	/** EN: Single-user read including tag memberships. PT-BR: Leitura de um utilizador com tags associadas. */
	@Query(
			"select distinct u from User u left join fetch u.tagAssignments ua left join fetch ua.tag "
					+ "left join fetch ua.assignedBy where u.id = :id")
	Optional<User> findWithTagsById(@Param("id") UUID id);

	/** EN: Lookup by {@code code} including tag memberships. PT-BR: Busca pelo {@code code} incluindo tags. */
	@Query(
			"select distinct u from User u left join fetch u.tagAssignments ua left join fetch ua.tag "
					+ "left join fetch ua.assignedBy where u.code = :code")
	Optional<User> findWithTagsByCode(@Param("code") String code);
}
