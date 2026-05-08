package com.rb.multi.agent.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rb.multi.agent.entity.Doctor;

/**
 * <p><b>EN:</b> Persistence for {@link Doctor} subtype and joined-table maintenance.</p>
 * <p><b>PT-BR:</b> Persistência da subtipo {@link Doctor} e sincronização da tabela ligada.</p>
 */
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

	@Modifying
	@Query(nativeQuery = true, value = "INSERT INTO doctors (id) VALUES (:userId) ON CONFLICT DO NOTHING")
	int insertDoctorRowIfAbsent(@Param("userId") UUID userId);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM doctors WHERE id = :userId")
	void deleteDoctorRow(@Param("userId") UUID userId);
}
