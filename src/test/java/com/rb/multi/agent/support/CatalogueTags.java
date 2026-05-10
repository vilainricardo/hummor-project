package com.rb.multi.agent.support;

import java.util.Locale;
import java.util.Set;

import com.rb.multi.agent.constants.TagCategory;
import com.rb.multi.agent.entity.Tag;

/**
 * EN: Builds {@link Tag} entities for tests using name-derived stable {@link Tag#getCode()} (same rule as migration backfill).
 * PT-BR: Tags de teste com código derivado do nome (alinhado à migração).
 */
public final class CatalogueTags {

	private CatalogueTags() {
	}

	public static String codeFromNameSlug(String nameSlug) {
		return nameSlug.toUpperCase(Locale.ROOT).replace('-', '_');
	}

	public static Tag seed(String nameSlug, String description, TagCategory category) {
		return seed(nameSlug, description, Set.of(category));
	}

	public static Tag seed(String nameSlug, String description, Set<TagCategory> categories) {
		return new Tag(codeFromNameSlug(nameSlug), nameSlug, description, categories);
	}
}
