package com.rb.multi.agent.constants;

/**
 * <p><b>EN:</b> Lifecycle of an established doctor–patient roster link (FR-003).</p>
 * <p><b>PT-BR:</b> Ciclo de vida do vínculo médico–paciente na lista (FR-003): activo vs desvinculado com ou sem acesso.</p>
 */
public enum DoctorPatientLinkStatus {

	/**
	 * EN: Normal linked state. PT-BR: Estado activo do vínculo.
	 */
	ACTIVE,

	/**
	 * EN: Unlinked; doctor may still see historical data (FR-003). PT-BR: Desvinculado com acesso ao histórico.
	 */
	UNLINKED_WITH_ACCESS,

	/**
	 * EN: Unlinked; access to patient data removed (FR-003). PT-BR: Desvinculado sem acesso.
	 */
	UNLINKED_WITHOUT_ACCESS
}
