package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Changing patient tag memberships requires {@code assignedByDoctorId} on the write request.</p>
 * <p><b>PT-BR:</b> Alterações às etiquetas do paciente exigem {@code assignedByDoctorId} no pedido.</p>
 */
public class TagAssignmentDoctorRequiredException extends RuntimeException {

	public TagAssignmentDoctorRequiredException() {
		super("tag assignment requires assignedByDoctorId when tag set changes");
	}
}
