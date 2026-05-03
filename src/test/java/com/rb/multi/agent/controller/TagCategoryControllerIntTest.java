package com.rb.multi.agent.controller;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.rb.multi.agent.entity.TagCategory;

/** EN: {@link TagCategoryController}. PT-BR: {@link TagCategoryController}. */
class TagCategoryControllerIntTest extends AbstractControllerIntTest {

	@Test
	@DisplayName("GET /api/v1/tag-categories returns stable enum enumeration")
	void list_returnsAllCategoriesWithDeclarationOrder() throws Exception {
		mockMvc.perform(get("/api/v1/tag-categories").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", arrayWithSize(TagCategory.values().length)))
				.andExpect(jsonPath("$[0]").value(TagCategory.values()[0].name()));
	}
}
