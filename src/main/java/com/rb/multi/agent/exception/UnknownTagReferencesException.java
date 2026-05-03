package com.rb.multi.agent.exception;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p><b>EN:</b> Write model referenced one or more tag UUIDs absent from <code>tags</code>.</p>
 * <p><b>PT-BR:</b> O modelo de escrita citou UUIDs de tags inexistentes em <code>tags</code>.</p>
 */
public final class UnknownTagReferencesException extends RuntimeException {

	private final Set<UUID> unknownIds;

	public UnknownTagReferencesException(Set<UUID> unknownIds) {
		super(
				"unknown tag ids: "
						+ unknownIds.stream()
								.sorted()
								.map(UUID::toString)
								.collect(Collectors.joining(", ")));
		this.unknownIds = Collections.unmodifiableSet(Set.copyOf(unknownIds));
	}

	/** EN: Snapshot of ids not resolving to rows. PT-BR: Snapshot dos ids sem linha correspondente. */
	public Set<UUID> getUnknownIds() {
		return unknownIds;
	}

	/** EN: Formatted UUID list for i18n placeholders. PT-BR: Lista UUID formatada para mensagens i18n. */
	public String formattedIds() {
		return unknownIds.stream().sorted().map(UUID::toString).collect(Collectors.joining(", "));
	}
}
