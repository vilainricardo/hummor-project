package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Catalogue tags may only be linked to accounts that represent patients ({@code isDoctor=false}).</p>
 * <p><b>PT-BR:</b> Etiquetas de catálogo só podem ser ligadas a contas paciente ({@code isDoctor=false}).</p>
 */
public class TagAssignmentPatientOnlyException extends RuntimeException {

	private final String targetCode;

	public TagAssignmentPatientOnlyException(String targetCode) {
		super("tag assignment only allowed for patient accounts target=" + targetCode);
		this.targetCode = targetCode;
	}

	public String targetCode() {
		return targetCode;
	}

	public Object[] messageArguments() {
		return new Object[] { targetCode != null ? targetCode : "" };
	}
}
