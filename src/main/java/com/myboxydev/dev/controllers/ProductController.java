package com.myboxydev.dev.controllers;

import com.myboxydev.dev.domain.enums.ProductMediaType;
import com.myboxydev.dev.dto.ProductCardDTO;
import com.myboxydev.dev.dto.ProductDTO;
import com.myboxydev.dev.dto.ProductMediaDTO;
import com.myboxydev.dev.dto.StoreSimpleDTO;
import com.myboxydev.dev.dto.SubcategoryDTO;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.ProductMediaRepository;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductRepository productRepository;
  private final ProductMediaRepository productMediaRepository;

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
                      product.getSubcategory().getCategory().getId(),
                      product.getSubcategory().getCategory().getName()
              );

              ProductDTO detailDTO = new ProductDTO(
                      product.getId(),
                      product.getName(),
                      product.getDescription(),
                      product.getPrice(),
                      product.getMainImage(),
                      product.getIsFavorite(),
                      product.getIsCustom(),
                      product.getInStock(),
                      product.getNumSells(),
                      product.getDiscount(),
                      product.getReviews(),
                      product.getWeight(),
                      product.getColor(),
                      product.getFormat(),
                      product.getDuration(),
                      product.getDimensions(),
                      product.getComposition(),
                      product.getMaterial(),
                      product.getObservations(),
                      storeDTO,
                      subcategoryDTO,
                      toMediaDTOs(product)
              );
              return ResponseEntity.ok(detailDTO);
            })
            .orElse(ResponseEntity.notFound().build());
  }

  static final int SIMILAR_MAX_LIMIT = 20;

  // Carrossel "Parecidos": produtos aleatórios da mesma subcategoria, sem o atual.
  // Produto sem subcategoria não tem parecidos (lista vazia).
  @GetMapping("/{id}/similar")
  public ResponseEntity<List<ProductCardDTO>> getSimilarProducts(
          @PathVariable UUID id,
          @RequestParam(defaultValue = "6") int limit
  ) {
    int safeLimit = Math.clamp(limit, 1, SIMILAR_MAX_LIMIT);
    return productRepository.findById(id)
            .map(product -> {
              if (product.getSubcategory() == null) {
                return ResponseEntity.ok(List.<ProductCardDTO>of());
              }
              List<ProductCardDTO> similar = productRepository
                      .findRandomBySubcategory(product.getSubcategory().getId(), id, safeLimit).stream()
                      .map(this::toCardDTO)
                      .collect(Collectors.toList());
              return ResponseEntity.ok(similar);
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

  // Galeria do carrossel. Produto ainda sem linhas em product_media usa a main_image
  // como única imagem, para o app sempre ter algo para mostrar.
  private List<ProductMediaDTO> toMediaDTOs(ProductModel product) {
    List<ProductMediaDTO> media = productMediaRepository.findByProductIdOrderByPositionAsc(product.getId()).stream()
            .map(m -> new ProductMediaDTO(m.getMediaType(), m.getUrl(), m.getThumbnailUrl()))
            .toList();
    if (media.isEmpty() && product.getMainImage() != null && !product.getMainImage().isBlank()) {
      return List.of(new ProductMediaDTO(ProductMediaType.IMAGE, product.getMainImage(), null));
    }
    return media;
  }

  private ProductCardDTO toCardDTO(ProductModel product) {
    return new ProductCardDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getMainImage(),
            product.getIsFavorite(),
            product.getIsCustom(),
            product.getInStock(),
            product.getNumSells(),
            product.getDiscount(),
            product.getReviews(),
            product.getWeight(),
            product.getColor(),
            product.getFormat(),
            product.getDuration(),
            product.getDimensions(),
            product.getComposition(),
            product.getMaterial(),
            product.getObservations(),
            product.getStore().getId(),
            product.getStore().getName(),
            product.getStore().getImage(),
            product.getStore().getRating()
    );
  }
}