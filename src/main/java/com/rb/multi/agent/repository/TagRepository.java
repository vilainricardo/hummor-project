package com.rb.multi.agent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.constants.TagCategory;

/**
 * <p><b>EN:</b> Persistence port for catalogue {@link Tag} rows keyed by UUID.</p>
 * <p><b>PT-BR:</b> Porta de persistência para linhas do catálogo {@link Tag} com chave UUID.</p>
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {

	Optional<Tag> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByCode(String code);

	@Query("SELECT DISTINCT t FROM Tag t JOIN t.categories c WHERE c = :category")
	List<Tag> findAllByCategory(@Param("category") TagCategory category);
}
