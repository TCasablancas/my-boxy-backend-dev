package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.CategoryDTO;
import com.myboxydev.dev.dto.SubcategoryDTO;
import com.myboxydev.dev.model.SubcategoryModel;
import com.myboxydev.dev.repository.CategoryRepository;
import com.myboxydev.dev.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    // Retorna todas as categorias já com as subcategorias aninhadas,
    // prontas pra montar menu de navegação/filtro no Flutter.
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        var categories = categoryRepository.findAllByOrderBySortOrderAsc();
        var subcategories = subcategoryRepository.findAllByOrderBySortOrderAsc();

        Map<UUID, List<SubcategoryDTO>> subcategoriesByCategory = subcategories.stream()
            .collect(Collectors.groupingBy(
                sub -> sub.getCategory().getId(),
                Collectors.mapping(this::toSubcategoryDTO, Collectors.toList())
            ));

        List<CategoryDTO> result = categories.stream()
            .map(category -> new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getSlug(),
                subcategoriesByCategory.getOrDefault(category.getId(), List.of())
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private SubcategoryDTO toSubcategoryDTO(SubcategoryModel sub) {
        return new SubcategoryDTO(sub.getId(), sub.getName(), sub.getSlug(), sub.getCategory().getId());
    }
}
