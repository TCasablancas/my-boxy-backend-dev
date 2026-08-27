package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.CategorySimpleDTO;
import com.myboxydev.dev.dto.StoreDTO;
import com.myboxydev.dev.dto.StoreSimpleDTO;
import com.myboxydev.dev.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "*")
public class StoreController {
  @Autowired
  private StoreRepository storeRepository;

  // Lista de lojas — usada na home, em "favoritos", etc.
  @GetMapping
  public ResponseEntity<List<StoreSimpleDTO>> listStores() {
    List<StoreSimpleDTO> stores = storeRepository.findAll().stream()
      .map(store -> new StoreSimpleDTO(store.getId(), store.getName(), store.getLocation(), store.getImage(), store.getRating()))
      .collect(Collectors.toList());
    return ResponseEntity.ok(stores);
  }

  // Página da loja, com as categorias que ela atende
  @GetMapping("/{id}")
  public ResponseEntity<StoreDTO> getStoreById(@PathVariable UUID id) {
    return storeRepository.findById(id)
      .map(store -> {
        List<CategorySimpleDTO> categories = store.getCategories().stream()
          .map(category -> new CategorySimpleDTO(category.getId(), category.getName(), category.getSlug()))
          .collect(Collectors.toList());

        StoreDTO dto = new StoreDTO(
          store.getId(),
          store.getName(),
          store.getLocation(),
          store.getImage(),
          store.getRating(),
          categories
        );
        return ResponseEntity.ok(dto);
      })
      .orElse(ResponseEntity.notFound().build());
  }
}