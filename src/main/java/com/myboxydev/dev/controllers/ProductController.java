package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ProductCardDTO;
import com.myboxydev.dev.dto.ProductDTO;
import com.myboxydev.dev.dto.StoreSimpleDTO;
import com.myboxydev.dev.dto.SubcategoryDTO;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.specification.ProductSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
  @Autowired
  private ProductRepository productRepository;

  // Home / Listagem / Busca / Navegação por categoria — tudo no mesmo endpoint,
  // já que os filtros são opcionais e combináveis.
  // Ex: /api/products                                -> lista tudo (home)
  //     /api/products?name=vaso                       -> busca por nome
  //     /api/products?categoryId=...                  -> navegação por categoria
  //     /api/products?subcategoryId=...                -> navegação por subcategoria
  //     /api/products?minPrice=100&maxPrice=300        -> filtro de faixa de preço
  //     /api/products?storeId=...                      -> produtos de uma loja
  // (todos os parâmetros podem ser combinados na mesma chamada)
  @GetMapping
  public ResponseEntity<List<ProductCardDTO>> listProducts(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) UUID storeId,
          @RequestParam(required = false) UUID categoryId,
          @RequestParam(required = false) UUID subcategoryId,
          @RequestParam(required = false) BigDecimal minPrice,
          @RequestParam(required = false) BigDecimal maxPrice
  ) {
    var spec = ProductSpecifications.build(name, storeId, categoryId, subcategoryId, minPrice, maxPrice);
    List<ProductCardDTO> products = productRepository.findAll(spec).stream()
            .map(this::toCardDTO)
            .collect(Collectors.toList());
    return ResponseEntity.ok(products);
  }

  // Página do Produto
  @GetMapping("/{id}")
  public ResponseEntity<ProductDTO> getProductById(@PathVariable UUID id) {
    return productRepository.findById(id)
            .map(product -> {
              StoreSimpleDTO storeDTO = new StoreSimpleDTO(
                      product.getStore().getId(),
                      product.getStore().getName(),
                      product.getStore().getLocation(),
                      product.getStore().getImage(),
                      product.getStore().getRating()
              );

              SubcategoryDTO subcategoryDTO = product.getSubcategory() == null ? null : new SubcategoryDTO(
                      product.getSubcategory().getId(),
                      product.getSubcategory().getName(),
                      product.getSubcategory().getSlug(),
                      product.getSubcategory().getCategory().getId()
              );

              ProductDTO detailDTO = new ProductDTO(
                      product.getId(),
                      product.getName(),
                      product.getDescription(),
                      product.getPrice(),
                      product.getMainImage(),
                      storeDTO,
                      subcategoryDTO
              );
              return ResponseEntity.ok(detailDTO);
            })
            .orElse(ResponseEntity.notFound().build());
  }

  // Carrossel de Itens Relacionados (mesma loja, excluindo o produto atual)
  // Antes esperava um "lojaId" que nunca vinha na URL — agora deriva do próprio produto.
  @GetMapping("/{id}/related")
  public ResponseEntity<List<ProductCardDTO>> getRelatedProducts(@PathVariable UUID id) {
    return productRepository.findById(id)
            .map(product -> {
              List<ProductCardDTO> related = productRepository
                      .findByStoreIdAndIdNot(product.getStore().getId(), id).stream()
                      .map(this::toCardDTO)
                      .collect(Collectors.toList());
              return ResponseEntity.ok(related);
            })
            .orElse(ResponseEntity.notFound().build());
  }

  private ProductCardDTO toCardDTO(ProductModel product) {
    return new ProductCardDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getMainImage(),
            product.getStore().getId(),
            product.getStore().getName(),
            product.getStore().getImage(),
            product.getStore().getRating()
    );
  }
}