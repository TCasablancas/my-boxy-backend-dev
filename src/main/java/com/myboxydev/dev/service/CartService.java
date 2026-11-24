package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.CartItemEntity;
import com.myboxydev.dev.dto.CartDTO;
import com.myboxydev.dev.dto.CartItemDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.repository.CartRepository;
import com.myboxydev.dev.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrinho do usuário logado, um item por produto. Definir a quantidade cria o
 * item ou troca a quantidade (idempotente); remover um item que não existe não dá erro.
 */
@Service
@RequiredArgsConstructor
public class CartService {
  public static final int MIN_QUANTITY = 1;
  public static final int MAX_QUANTITY = 99;

  private final CartRepository cartRepository;
  private final ProductRepository productRepository;

  @Transactional(readOnly = true)
  public CartDTO getCart(UUID userId) {
    List<CartItemEntity> items = cartRepository.findByUserIdOrderByCreatedAtDescProductIdDesc(userId);
    Map<UUID, ProductModel> products = productRepository
            .findAllById(items.stream().map(CartItemEntity::getProductId).toList()).stream()
            .collect(Collectors.toMap(ProductModel::getId, Function.identity()));
    return new CartDTO(items.stream()
            .filter(item -> products.containsKey(item.getProductId()))
            .map(item -> toDto(item, products.get(item.getProductId())))
            .toList());
  }

  @Transactional
  public CartItemDTO setQuantity(UUID userId, UUID productId, Integer quantity) {
    if (quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
      throw new FieldValidationException("quantity",
              "A quantidade deve ficar entre " + MIN_QUANTITY + " e " + MAX_QUANTITY);
    }
    ProductModel product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    CartItemEntity item = cartRepository.findById(new CartItemEntity.Key(userId, productId))
            .orElseGet(() -> CartItemEntity.builder()
                    .userId(userId)
                    .productId(productId)
                    .createdAt(now)
                    .build());
    item.setQuantity(quantity);
    item.setUpdatedAt(now);
    return toDto(cartRepository.save(item), product);
  }

  @Transactional
  public void remove(UUID userId, UUID productId) {
    cartRepository.deleteItem(userId, productId);
  }

  private static CartItemDTO toDto(CartItemEntity item, ProductModel product) {
    StoreModel store = product.getStore();
    return new CartItemDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getMainImage(),
            item.getQuantity(),
            product.getInStock(),
            store == null ? null : store.getId(),
            store == null ? null : store.getName(),
            store == null ? null : store.getImage());
  }
}
