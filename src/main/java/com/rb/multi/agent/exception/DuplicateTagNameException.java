package com.rb.multi.agent.exception;

/**
 * <p><b>EN:</b> Violates uniqueness of normalized tag slug already stored catalogue-wide.</p>
 * <p><b>PT-BR:</b> Viola unicidade do nome normalizado já existente no catálogo global.</p>
 */
public class DuplicateTagNameException extends RuntimeException {

	private final String name;

	public DuplicateTagNameException(String name) {
		super("duplicate tag name " + name);
		this.name = name;
	}

	/** EN: Offending persisted identifier token. PT-BR: Identificador (nome) em conflito. */
	public String getName() {
		return name;
	}
}
