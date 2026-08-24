package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ProductCardDTO;
import com.myboxydev.dev.dto.ProductDTO;
import com.myboxydev.dev.dto.StoreSimpleDTO;
import com.myboxydev.dev.repository.ProductRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produto")
@CrossOrigin(origins = "*")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    // Home / Listagem Geral
    @GetMapping
    public ResponseEntity<List<ProductCardDTO>> getAllProducts() {
        List<ProductCardDTO> products = productRepository.findAll().stream()
            .map(product -> new ProductCardDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getMainImage(),
                product.getLoja().getId()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    // Pagina do Produto
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable UUID id) {
        return productRepository.findById(id)
            .map(product -> {
                StoreSimpleDTO storeDTO = new StoreSimpleDTO(
                    product.getLoja().getId(),
                    product.getLoja().getName(),
                    product.getLoja().getLocation(),
                    product.getLoja().getImage()
                );
                ProductDTO detailDTO = new ProductDTO(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getMainImage(), storeDTO
                );
                return ResponseEntity.ok(detailDTO);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Carrossel de Itens Relacionados
    @GetMapping("/{id}/relacionados")
    public ResponseEntity<List<ProductCardDTO>> getRelatedProducts(@PathVariable UUID lojaId, @PathVariable UUID id) {
        List<ProductCardDTO> related = productRepository.findByLojaIdAndIdNot(lojaId, id).stream()
            .map(product -> new ProductCardDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getMainImage(),
                product.getLoja().getId()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(related);
    }

    // Busca
    @GetMapping("/busca")
    public ResponseEntity<List<ProductCardDTO>> searchProducts(@RequestParam String query) {
        List<ProductCardDTO> results = productRepository.findByNameContainingIgnoreCase(query).stream()
            .map(product -> new ProductCardDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getMainImage(),
                product.getLoja().getId()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}
