package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Another catalogue row already uses this tag {@code code}.</p>
 * <p><b>PT-BR:</b> Outra linha do catálogo já usa este {@code code} de etiqueta.</p>
 */
public class DuplicateTagCodeException extends RuntimeException {

	private final String code;

	public DuplicateTagCodeException(String code) {
		super("duplicate tag code " + code);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
