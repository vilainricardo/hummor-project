package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> The user referenced by {@code assignedByDoctorId} exists but {@code isDoctor} is {@code false}.</p>
 * <p><b>PT-BR:</b> O utilizador de {@code assignedByDoctorId} existe mas {@code isDoctor} é {@code false}.</p>
 */
public class AssigningActorNotDoctorException extends RuntimeException {

	private final String actorCode;

	public AssigningActorNotDoctorException(String actorCode) {
		super("user is not a doctor for tag assignment actor=" + actorCode);
		this.actorCode = actorCode;
	}

	public String actorCode() {
		return actorCode;
	}

	public Object[] messageArguments() {
		return new Object[] { actorCode != null ? actorCode : "" };
	}
}
