package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Update payload {@code code} does not match the persisted catalogue row (code is immutable).</p>
 * <p><b>PT-BR:</b> O {@code code} no payload não coincide com a linha persistida (imutável).</p>
 */
public class TagCodeMismatchException extends RuntimeException {

	private final String expectedCode;
	private final String actualCode;

	public TagCodeMismatchException(String expectedCode, String actualCode) {
		super("tag code mismatch: expected " + expectedCode + ", got " + actualCode);
		this.expectedCode = expectedCode;
		this.actualCode = actualCode;
	}

	public String getExpectedCode() {
		return expectedCode;
	}

	public String getActualCode() {
		return actualCode;
	}
}
