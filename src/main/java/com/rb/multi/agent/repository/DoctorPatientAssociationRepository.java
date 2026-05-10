package com.rb.multi.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rb.multi.agent.entity.DoctorPatientAssociation;
import com.rb.multi.agent.entity.DoctorPatientKey;

/**
 * <p><b>EN:</b> Persistence for {@code doctor_patients} rows (FR-004 dates, FR-003 status).</p>
 * <p><b>PT-BR:</b> Persistência das linhas em {@code doctor_patients} (FR-004, FR-003).</p>
 */
public interface DoctorPatientAssociationRepository extends JpaRepository<DoctorPatientAssociation, DoctorPatientKey> {
}
