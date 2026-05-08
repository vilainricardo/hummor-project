package com.rb.multi.agent.dto;

/**
 * <p><b>EN:</b> Snapshot after a mutual-link step — roster is updated only once both halves are true unless already linked.</p>
 * <p><b>PT-BR:</b> Estado após um passo de vínculo mútuo; a lista só muda quando ambos estão verdadeiros (ou já estava ligado).</p>
 */
public record MutualDoctorPatientLinkResponse(
		boolean rosterLinkedNow,
		boolean patientAcknowledged,
		boolean doctorAcknowledged) {

	public static MutualDoctorPatientLinkResponse alreadyOnRoster() {
		return new MutualDoctorPatientLinkResponse(true, true, true);
	}
}
