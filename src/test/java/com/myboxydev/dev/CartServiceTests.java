package com.myboxydev.dev;

import com.myboxydev.dev.dto.CartDTO;
import com.myboxydev.dev.dto.CartItemDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.service.CartService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Queries reais no H2 (modo PostgreSQL).
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceTests {
  @Autowired
  private CartService service;

  @Autowired
  private EntityManager em;

  private StoreModel store;
  private ProductModel vase;
  private ProductModel table;
  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    store = new StoreModel();
    store.setName("Ateliê");
    store.setLocation("Santos");
    store.setImage("https://cdn.test/loja.webp");
    store.setRating(BigDecimal.ONE);
    em.persist(store);
    vase = product("vaso");
    table = product("mesa");
    em.flush();
  }

  @Test
  void setQuantityCreatesThenUpdatesWithoutDuplicating() {
    CartItemDTO created = service.setQuantity(userId, vase.getId(), 1);
    assertThat(created.quantity()).isEqualTo(1);
    assertThat(created.name()).isEqualTo("vaso");
    assertThat(created.unitPrice()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(created.storeName()).isEqualTo("Ateliê");

    service.setQuantity(userId, vase.getId(), 4);
    em.flush();
    em.clear();

    CartDTO cart = service.getCart(userId);
    assertThat(cart.items()).hasSize(1);
    assertThat(cart.items().get(0).quantity()).isEqualTo(4);
  }

  @Test
  void cartIsPerUserAndRemoveIsIdempotent() {
    service.setQuantity(userId, vase.getId(), 1);
    service.setQuantity(userId, table.getId(), 2);
    service.setQuantity(otherUserId, table.getId(), 1);
    em.flush();

    assertThat(service.getCart(userId).items()).extracting(CartItemDTO::productId)
            .containsExactlyInAnyOrder(vase.getId(), table.getId());
    assertThat(service.getCart(otherUserId).items()).hasSize(1);

    service.remove(userId, vase.getId());
    service.remove(userId, vase.getId());
    em.flush();
    em.clear();

    assertThat(service.getCart(userId).items()).extracting(CartItemDTO::productId).containsExactly(table.getId());
    assertThat(service.getCart(otherUserId).items()).hasSize(1);
  }

  @Test
  void quantityOutOfRangeIsRejected() {
    assertThatThrownBy(() -> service.setQuantity(userId, vase.getId(), 0)).isInstanceOf(FieldValidationException.class);
    assertThatThrownBy(() -> service.setQuantity(userId, vase.getId(), 100)).isInstanceOf(FieldValidationException.class);
    assertThatThrownBy(() -> service.setQuantity(userId, vase.getId(), null)).isInstanceOf(FieldValidationException.class);
  }

  @Test
  void unknownProductIsNotFound() {
    assertThatThrownBy(() -> service.setQuantity(userId, UUID.randomUUID(), 1))
            .isInstanceOf(ResourceNotFoundException.class);
  }

  private ProductModel product(String name) {
    ProductModel p = new ProductModel();
    p.setName(name);
    p.setPrice(BigDecimal.TEN);
    p.setMainImage("https://cdn.test/" + name + ".webp");
    p.setIsFavorite(false);
    p.setInStock(true);
    p.setNumSells(0);
    p.setDiscount(BigDecimal.ZERO);
    p.setReviews(0);
    p.setStore(store);
    em.persist(p);
    return p;
  }
}
