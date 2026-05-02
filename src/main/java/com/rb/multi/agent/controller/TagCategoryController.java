package com.rb.multi.agent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rb.multi.agent.entity.TagCategory;

/**
 * <p><b>EN:</b> Exposes {@link TagCategory} constants for dropdowns/filters outside {@code /tags/{uuid}} routes.</p>
 * <p><b>PT-BR:</b> Expõe constantes {@link TagCategory} para filtros/UI fora das rotas {@code /tags/{uuid}}.</p>
 */
@RestController
@RequestMapping("/api/v1/tag-categories")
public class TagCategoryController {

	/**
	 * <p><b>EN:</b> All {@link TagCategory} values for pickers aligned with catalogue filtering.</p>
	 * <p><b>PT-BR:</b> Todos os valores de {@link TagCategory} para seletores alinhados com filtros do catálogo.</p>
	 */
	@GetMapping
	public TagCategory[] list() {
		return TagCategory.values();
	}
}
