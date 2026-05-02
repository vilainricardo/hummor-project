package com.rb.multi.agent.exception;

public class DuplicateUserCodeException extends RuntimeException {

	private final String code;

	public DuplicateUserCodeException(String code) {
		super("duplicate user code " + code);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
