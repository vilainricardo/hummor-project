package com.rb.multi.agent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.rb.multi.agent.constants.TagCategory;
import com.rb.multi.agent.dto.TagResponse;
import com.rb.multi.agent.entity.Tag;

/**
 * <p><b>EN:</b> Builds {@link TagResponse} with {@code name} and {@code description} from {@code tag.catalog.{code}.*}
 * keys, falling back to persisted catalogue text.</p>
 * <p><b>PT-BR:</b> Constrói DTO com nome/descrição a partir de chaves i18n e fallback na BD.</p>
 */
@Component
public class TagCatalogPresenter {

	public static final String MESSAGE_NAME_PREFIX = "tag.catalog.";
	public static final String MESSAGE_NAME_SUFFIX = ".name";
	public static final String MESSAGE_DESCRIPTION_SUFFIX = ".description";

	private final MessageSource messageSource;

	public TagCatalogPresenter(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/** EN: Localized read projection. PT-BR: Projeção de leitura localizada. */
	public TagResponse present(Tag tag) {
		Locale locale = LocaleContextHolder.getLocale();
		return new TagResponse(
				tag.getId(),
				tag.getCode(),
				resolveName(tag, locale),
				resolveDescription(tag, locale),
				sortedCategories(tag),
				tag.getCreatedAt(),
				null,
				null);
	}

	/** EN: Tag row plus clinician-assignment slice metadata. PT-BR: Tag com metadados da fatia do médico. */
	public TagResponse presentClinicianAssignment(Tag tag, boolean criticalForClinician, UUID assignedByClinicianId) {
		TagResponse base = present(tag);
		return new TagResponse(
				base.id(),
				base.code(),
				base.name(),
				base.description(),
				base.categories(),
				base.createdAt(),
				criticalForClinician,
				assignedByClinicianId);
	}

	private static List<TagCategory> sortedCategories(Tag tag) {
		return tag.getCategories().stream()
				.sorted(Comparator.comparing(Enum::name))
				.collect(Collectors.toList());
	}

	private String resolveName(Tag tag, Locale locale) {
		String key = MESSAGE_NAME_PREFIX + tag.getCode() + MESSAGE_NAME_SUFFIX;
		return messageSource.getMessage(key, null, tag.getName(), locale);
	}

	private String resolveDescription(Tag tag, Locale locale) {
		String raw = tag.getDescription();
		if (raw == null) {
			return null;
		}
		String key = MESSAGE_NAME_PREFIX + tag.getCode() + MESSAGE_DESCRIPTION_SUFFIX;
		return messageSource.getMessage(key, null, raw, locale);
	}
}
