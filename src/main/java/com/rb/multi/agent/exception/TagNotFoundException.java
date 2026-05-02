package com.rb.multi.agent.exception;

import java.util.UUID;

/**
 * <p><b>EN:</b> Raised when referencing a catalogue tag absent by surrogate id or canonical name lookup.</p>
 * <p><b>PT-BR:</b> Lançada ao referenciar etiqueta de catálogo inexistente por id surrogate ou pelo nome.</p>
 */
public class TagNotFoundException extends RuntimeException {

	private final UUID tagId;
	private final String name;

	private TagNotFoundException(UUID tagId, String name, String developerMessage) {
		super(developerMessage);
		this.tagId = tagId;
		this.name = name;
	}

	/** EN: Factory for missing-tag-by-id semantics. PT-BR: Fábrica para etiqueta inexistente pelo id. */
	public static TagNotFoundException byId(UUID id) {
		return new TagNotFoundException(id, null, "tag not found id " + id);
	}

	/** EN: Factory for missing-tag-by-name semantics. PT-BR: Fábrica para etiqueta inexistente pelo nome. */
	public static TagNotFoundException byName(String name) {
		String trimmed = name != null ? name.strip() : "";
		return new TagNotFoundException(null, trimmed.isEmpty() ? null : trimmed, "tag not found name " + trimmed);
	}

	/**
	 * <p><b>EN:</b> {@code true} if lookup used persistence UUID; {@code false} when canonical {@code name} was used.</p>
	 * <p><b>PT-BR:</b> {@code true} se a consulta foi por UUID de persistência; {@code false} se foi pelo nome canónico.</p>
	 */
	public boolean resolvedByUuid() {
		return tagId != null;
	}

	/** EN: Ordered args for localized {@code MessageFormat}. PT-BR: Argumentos ordenados para {@code MessageFormat} localizada. */
	public Object[] messageArguments() {
		return resolvedByUuid() ? new Object[] { tagId.toString() } : new Object[] { name != null ? name : "" };
	}
}
