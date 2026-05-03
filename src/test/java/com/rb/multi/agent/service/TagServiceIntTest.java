package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.TagWriteRequest;
import com.rb.multi.agent.entity.Tag;
import com.rb.multi.agent.entity.TagCategory;
import com.rb.multi.agent.exception.DuplicateTagNameException;
import com.rb.multi.agent.exception.TagNotFoundException;
import com.rb.multi.agent.repository.TagRepository;
import com.rb.multi.agent.repository.UserRepository;

/**
 * EN: {@link TagService} catalogue rules and retrieval edge coverage.
 * PT-BR: Cobertura de regras do catálogo e casos-limites em {@link TagService}.
 */
@SpringBootTest
@Transactional
class TagServiceIntTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void purge() {
		userRepository.deleteAll();
		tagRepository.deleteAll();
	}

	@Test
	@DisplayName("findAll(null) vs filtro por categoria")
	void findAll_respectsNullableCategoryPredicate() {
		tagRepository.save(new Tag("wake", null, TagCategory.SLEEP));
		tagRepository.save(new Tag("worry", null, TagCategory.ANXIETY));

		assertThat(tagService.findAll(null)).hasSize(2);
		assertThat(tagService.findAll(TagCategory.SLEEP)).extracting(Tag::getName).containsExactly("wake");
	}

	@Test
	@DisplayName("findById aleatório devolve Optional vazio sem exceções")
	void findById_missing_returnsOptionalEmpty() {
		assertThat(tagService.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	@DisplayName("findByName ignorando caixa quando existente")
	void findByNameIgnoreCase_trimmedNormalization() {
		tagRepository.save(new Tag("CamelCaseSeed", null, TagCategory.SLEEP));

		var found = tagService.findByNameIgnoreCase("  camelCASEseed ").orElseThrow();
		assertThat(found.getName()).isEqualTo("CamelCaseSeed");
	}

	@Test
	@DisplayName("findByName null, em branco ou string vazia strip → Optional vazio")
	void findByName_blankOrNull_returnsEmpty() {
		assertThat(tagService.findByNameIgnoreCase(null)).isEmpty();
		assertThat(tagService.findByNameIgnoreCase("   ")).isEmpty();
		assertThat(tagService.findByNameIgnoreCase("")).isEmpty();
	}

	@Test
	@DisplayName("create normaliza espaços extras e collapse descrição em branco a null persistido")
	void create_trimNameAndCollapseBlankDescriptions() {
		var persisted =
				tagService.create(new TagWriteRequest(" spaced-name ", "     ", TagCategory.LOW_MOOD));
		tagRepository.flush();
		tagRepository.clear();

		var reloaded = tagRepository.findById(persisted.getId()).orElseThrow();
		assertThat(reloaded.getName()).isEqualTo("spaced-name");
		assertThat(reloaded.getDescription()).isNull();
	}

	@Test
	@DisplayName("nome apenas brancos após strip → IllegalArgumentException")
	void create_blankName_throwsIllegalArgument() {
		assertThatThrownBy(() -> tagService.create(new TagWriteRequest("\t\r\n ", null, TagCategory.OTHER))).isInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	@DisplayName("nome maior que limite dominial → IllegalArgumentException")
	void create_longName_throwsIllegalArgument() {
		assertThatThrownBy(
				() -> tagService.create(
						new TagWriteRequest(
								"a".repeat(51),
								null,
								TagCategory.SLEEP))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("categoria obrigatória no update → NullPointerException")
	void update_whenCategoryMissing_throwsNpeViaRequireNonNull() {
		var surrogate = tagRepository.save(new Tag("needs-category", null, TagCategory.SLEEP)).getId();
		assertThatThrownBy(() -> tagService.update(surrogate, new TagWriteRequest("rename", null, null))).isInstanceOf(
				NullPointerException.class);
	}

	@Test
	@DisplayName("duplicação global insensível a maiúsculas tanto no insert como no update contra terceiros")
	void duplicateInsensitiveName_conflictOnCreateAndUpdateAgainstOtherRows() {
		tagRepository.save(new Tag("Primary", null, TagCategory.SLEEP));

		assertThatThrownBy(() -> tagService.create(new TagWriteRequest("primary", null, TagCategory.SLEEP))).isInstanceOfSatisfying(
				DuplicateTagNameException.class,
				ex -> assertThat(ex.getName()).isEqualTo("primary"));

		var survivor = tagRepository.save(new Tag("Survivor", null, TagCategory.SLEEP));

		assertThatThrownBy(
				() -> tagService.update(
						survivor.getId(),
						new TagWriteRequest(
								"PRIMARY",
								null,
								TagCategory.SLEEP))).isInstanceOf(DuplicateTagNameException.class);
	}

	@Test
	@DisplayName("delete id absent → TagNotFoundException")
	void delete_unknownId_raisesTagMissing() {
		assertThatThrownBy(() -> tagService.deleteById(UUID.randomUUID())).isInstanceOf(TagNotFoundException.class);
	}

	@Test
	@DisplayName("update id absent → TagNotFoundException.byId")
	void update_unknown_raisesTagMissing() {
		assertThatThrownBy(
				() -> tagService.update(
						UUID.randomUUID(),
						new TagWriteRequest("ghost", null, TagCategory.SLEEP))).isInstanceOf(TagNotFoundException.class);
	}
}
