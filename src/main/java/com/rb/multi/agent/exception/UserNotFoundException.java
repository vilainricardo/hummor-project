package com.rb.multi.agent.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

	private final UUID userId;
	private final String publicCode;

	private UserNotFoundException(UUID userId, String publicCode, String developerMessage) {
		super(developerMessage);
		this.userId = userId;
		this.publicCode = publicCode;
	}

	public static UserNotFoundException byId(UUID id) {
		return new UserNotFoundException(id, null, "missing user id " + id);
	}

	public static UserNotFoundException byCode(String code) {
		return new UserNotFoundException(null, code, "missing user code " + code);
	}

	public boolean lookupById() {
		return userId != null;
	}

	public Object[] messageArguments() {
		return lookupById() ? new Object[] { userId.toString() } : new Object[] { publicCode };
	}
}
