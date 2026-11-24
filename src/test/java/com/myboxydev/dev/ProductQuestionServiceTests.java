package com.myboxydev.dev;

import com.myboxydev.dev.dto.ProductQuestionDTO;
import com.myboxydev.dev.dto.ProductQuestionPageDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.service.ProductQuestionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Queries reais no H2 (modo PostgreSQL): insert nativo + página por cursor.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductQuestionServiceTests {
  // Mesma ordem da query: created_at desc, empate por id desc
  private static final Comparator<ProductQuestionDTO> NEWEST_FIRST = Comparator
          .comparing(ProductQuestionDTO::createdAt).thenComparing(ProductQuestionDTO::id).reversed();

  @Autowired
  private ProductQuestionService service;

  @Autowired
  private EntityManager em;

  private ProductModel product;
  private ProductModel otherProduct;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    StoreModel store = new StoreModel();
    store.setName("Ateliê");
    store.setLocation("Santos");
    store.setImage("https://cdn.test/loja.webp");
    store.setRating(BigDecimal.ONE);
    em.persist(store);
    product = product(store, "vaso");
    otherProduct = product(store, "mesa");
    em.flush();
  }

  @Test
  void askTrimsTextAndListsNewestFirstAcrossPages() {
    List<UUID> created = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      created.add(service.ask(userId, product.getId(), "  pergunta " + i + "  ").id());
    }
    service.ask(userId, otherProduct.getId(), "de outro produto");
    em.flush();

    ProductQuestionPageDTO first = service.list(product.getId(), null, 2);
    assertThat(first.items()).hasSize(2);
    assertThat(first.hasMore()).isTrue();
    assertThat(first.items()).extracting(ProductQuestionDTO::question).allMatch(q -> q.matches("pergunta \\d"));
    // Sem perfil no banco: nome anônimo, nunca dado pessoal
    assertThat(first.items().get(0).author().displayName()).isEqualTo("Usuário MyBoxy");

    ProductQuestionPageDTO second = service.list(product.getId(), first.nextCursor(), 2);
    assertThat(second.items()).hasSize(1);
    assertThat(second.hasMore()).isFalse();
    assertThat(second.nextCursor()).isNull();

    // As duas páginas juntas: todas as perguntas do produto, sem repetir, mais recentes primeiro
    List<ProductQuestionDTO> all = new ArrayList<>(first.items());
    all.addAll(second.items());
    assertThat(all).extracting(ProductQuestionDTO::id).containsExactlyInAnyOrderElementsOf(created);
    assertThat(all).isSortedAccordingTo(NEWEST_FIRST);
  }

  @Test
  void askRejectsBlankAndTooLongText() {
    assertThatThrownBy(() -> service.ask(userId, product.getId(), "   "))
            .isInstanceOf(FieldValidationException.class);
    assertThatThrownBy(() -> service.ask(userId, product.getId(), "a".repeat(501)))
            .isInstanceOf(FieldValidationException.class);
  }

  @Test
  void unknownProductIsNotFound() {
    assertThatThrownBy(() -> service.list(UUID.randomUUID(), null, null))
            .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.ask(userId, UUID.randomUUID(), "oi"))
            .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void invalidLimitIsRejected() {
    assertThatThrownBy(() -> service.list(product.getId(), null, 0)).isInstanceOf(FieldValidationException.class);
    assertThatThrownBy(() -> service.list(product.getId(), null, 51)).isInstanceOf(FieldValidationException.class);
  }

  private ProductModel product(StoreModel store, String name) {
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
