package com.rb.multi.agent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.DoctorPatientMutualLink;

/**
 * <p><b>EN:</b> Pending mutual links before roster membership.</p>
 * <p><b>PT-BR:</b> Ligações mútuas pendentes antes da lista do médico.</p>
 */
public interface DoctorPatientMutualLinkRepository extends JpaRepository<DoctorPatientMutualLink, UUID> {

	Optional<DoctorPatientMutualLink> findByPatient_IdAndDoctor_Id(UUID patientId, UUID doctorId);
}
