package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.ReviewEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.domain.enums.ReviewStatus;
import com.myboxydev.dev.domain.enums.ReviewTarget;
import com.myboxydev.dev.domain.enums.UserStatus;
import com.myboxydev.dev.dto.ReviewDTO;
import com.myboxydev.dev.dto.ReviewPageDTO;
import com.myboxydev.dev.dto.ReviewSummaryDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.ReviewRepository;
import com.myboxydev.dev.repository.StoreRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceTests {
  private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 11, 1, 12, 0, 0, 0, ZoneOffset.UTC);

  @Autowired private ReviewService reviewService;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private StoreRepository storeRepository;
  @Autowired private UserProfileRepository userProfileRepository;

  private StoreModel store;
  private ProductModel product;
  private ProductModel otherProduct;
  private int userSeq;

  @BeforeEach
  void setUp() {
    store = new StoreModel();
    store.setName("Ateliê Teste");
    store.setLocation("Santos - SP");
    store.setImage("https://cdn.test/store.webp");
    store.setRating(BigDecimal.ZERO);
    store = storeRepository.save(store);
    product = productRepository.save(newProduct("Vaso"));
    otherProduct = productRepository.save(newProduct("Caneca"));
  }

  @Test
  void summaryAlwaysHasFiveBucketsFromFiveToOne() {
    review(product, user("Ana Maria Souza"), 5, T0);
    review(product, user("Bruno Lima"), 5, T0.plusMinutes(1));
    review(product, user("Carla Dias"), 4, T0.plusMinutes(2));
    review(product, user("Davi Reis"), 1, T0.plusMinutes(3));
    // Não entram: outra avaliação escondida e avaliação de outro produto
    ReviewEntity hidden = review(product, user("Eva Melo"), 1, T0.plusMinutes(4));
    hidden.setStatus(ReviewStatus.HIDDEN);
    review(otherProduct, user("Fabio Rocha"), 2, T0);

    ReviewSummaryDTO summary = reviewService.getSummary(ReviewTarget.PRODUCT, product.getId());

    assertThat(summary.totalReviews()).isEqualTo(4);
    assertThat(summary.averageRating()).isEqualTo(3.8); // (5+5+4+1)/4 = 3.75
    assertThat(summary.distribution()).extracting(ReviewSummaryDTO.RatingBucket::rating)
            .containsExactly(5, 4, 3, 2, 1);
    assertThat(summary.distribution()).extracting(ReviewSummaryDTO.RatingBucket::count)
            .containsExactly(2L, 1L, 0L, 0L, 1L);
    assertThat(summary.distribution().get(0).percentage()).isEqualTo(50.0);
    assertThat(summary.distribution().get(1).percentage()).isEqualTo(25.0);
  }

  @Test
  void summaryWithoutReviewsHasNullAverage() {
    ReviewSummaryDTO summary = reviewService.getSummary(ReviewTarget.PRODUCT, product.getId());
    assertThat(summary.totalReviews()).isZero();
    assertThat(summary.averageRating()).isNull();
    assertThat(summary.distribution()).hasSize(5).allMatch(b -> b.count() == 0 && b.percentage() == 0.0);
  }

  @Test
  void storeSummaryIncludesProductAndStoreReviews() {
    review(product, user("Ana Souza"), 5, T0);
    review(otherProduct, user("Bruno Lima"), 3, T0);
    review(null, user("Carla Dias"), 4, T0); // avaliação direta da loja

    ReviewSummaryDTO summary = reviewService.getSummary(ReviewTarget.STORE, store.getId());
    assertThat(summary.totalReviews()).isEqualTo(3);
    assertThat(summary.averageRating()).isEqualTo(4.0);
  }

  @Test
  void unknownProductIs404() {
    assertThatThrownBy(() -> reviewService.getSummary(ReviewTarget.PRODUCT, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, UUID.randomUUID(), null, null, null))
            .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void cursorWalksEveryReviewOnceNewestFirst() {
    List<UUID> expected = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      // Dois pares com o mesmo created_at para exercitar o desempate pelo id
      expected.add(review(product, user("Pessoa " + i), 1 + (i % 5), T0.plusMinutes(i / 2)).getId());
    }

    List<UUID> seen = new ArrayList<>();
    String cursor = null;
    int pages = 0;
    do {
      ReviewPageDTO page = reviewService.list(ReviewTarget.PRODUCT, product.getId(), null, cursor, 3);
      page.items().forEach(i -> seen.add(i.id()));
      assertThat(page.hasMore()).isEqualTo(page.nextCursor() != null);
      cursor = page.nextCursor();
      pages++;
    } while (cursor != null && pages < 10);

    assertThat(pages).isEqualTo(3);
    assertThat(seen).hasSize(7).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(expected);
    List<ReviewEntity> ordered = seen.stream().map(id -> reviewRepository.findById(id).orElseThrow()).toList();
    for (int i = 1; i < ordered.size(); i++) {
      assertThat(ordered.get(i).getCreatedAt()).isBeforeOrEqualTo(ordered.get(i - 1).getCreatedAt());
    }
  }

  @Test
  void filterByOneOrMoreRatings() {
    review(product, user("Ana Souza"), 5, T0);
    review(product, user("Bruno Lima"), 4, T0.plusMinutes(1));
    review(product, user("Carla Dias"), 3, T0.plusMinutes(2));
    review(product, user("Davi Reis"), 5, T0.plusMinutes(3));

    assertThat(reviewService.list(ReviewTarget.PRODUCT, product.getId(), "5", null, null).items())
            .extracting(ReviewDTO::rating).containsExactly(5, 5);
    assertThat(reviewService.list(ReviewTarget.PRODUCT, product.getId(), "5, 4", null, null).items())
            .extracting(ReviewDTO::rating).containsExactly(5, 4, 5);
    assertThat(reviewService.list(ReviewTarget.PRODUCT, product.getId(), "", null, null).items()).hasSize(4);
  }

  @Test
  void cardCarriesAuthorCountersAndFlags() {
    UserProfileEntity author = user("Ana Maria de Souza");
    author.setAvatarUrl("https://cdn.test/ana.webp");
    ReviewEntity r = review(product, author, 5, T0);
    r.setComment("Lindo!");
    r.setLikesCount(12);
    r.setPhotosCount((short) 3);
    r.setOrderItemId(UUID.randomUUID());
    r.setEditedAt(T0.plusDays(1));
    review(product, anonymized(), 2, T0.minusMinutes(1));

    List<ReviewDTO> items = reviewService.list(ReviewTarget.PRODUCT, product.getId(), null, null, null).items();

    ReviewDTO card = items.get(0);
    assertThat(card.comment()).isEqualTo("Lindo!");
    assertThat(card.likesCount()).isEqualTo(12);
    assertThat(card.photosCount()).isEqualTo(3);
    assertThat(card.verifiedPurchase()).isTrue();
    assertThat(card.edited()).isTrue();
    assertThat(card.author().displayName()).isEqualTo("Ana S.");
    assertThat(card.author().alias()).isEqualTo(author.getAlias());
    assertThat(card.author().avatarUrl()).isEqualTo("https://cdn.test/ana.webp");

    ReviewDTO anon = items.get(1);
    assertThat(anon.author().displayName()).isEqualTo("Usuário MyBoxy");
    assertThat(anon.author().alias()).isNull();
    assertThat(anon.author().avatarUrl()).isNull();
    assertThat(anon.verifiedPurchase()).isFalse();
    assertThat(anon.edited()).isFalse();
  }

  @Test
  void invalidParamsPointToTheField() {
    UUID id = product.getId();
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, id, "6", null, null))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("ratings"));
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, id, "cinco", null, null))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("ratings"));
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, id, null, "lixo", null))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("cursor"));
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, id, null, null, 51))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("limit"));
    assertThatThrownBy(() -> reviewService.list(ReviewTarget.PRODUCT, id, null, null, 0))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("limit"));
  }

  private ProductModel newProduct(String name) {
    ProductModel p = new ProductModel();
    p.setName(name);
    p.setPrice(new BigDecimal("99.90"));
    p.setMainImage("https://cdn.test/p.webp");
    p.setIsFavorite(false);
    p.setInStock(true);
    p.setNumSells(0);
    p.setDiscount(BigDecimal.ZERO);
    p.setReviews(0);
    p.setStore(store);
    return p;
  }

  private UserProfileEntity user(String fullName) {
    int n = ++userSeq;
    return userProfileRepository.save(UserProfileEntity.builder()
            .id(UUID.randomUUID())
            .fullName(fullName)
            .alias("user" + n + "_" + UUID.randomUUID().toString().substring(0, 8))
            .cpfEncrypted("enc")
            .cpfHash(UUID.randomUUID().toString().replace("-", ""))
            .email("u" + n + "_" + UUID.randomUUID() + "@test.dev")
            .hasCpfRegistered(true)
            .hasStoreRegistered(false)
            .isSeller(false)
            .status(UserStatus.ACTIVE)
            .build());
  }

  private UserProfileEntity anonymized() {
    UserProfileEntity u = user("Nome Real Escondido");
    u.setStatus(UserStatus.ANONYMIZED);
    return u;
  }

  private ReviewEntity review(ProductModel p, UserProfileEntity author, int rating, OffsetDateTime at) {
    return reviewRepository.save(ReviewEntity.builder()
            .authorId(author.getId())
            .storeId(store.getId())
            .productId(p == null ? null : p.getId())
            .rating((short) rating)
            .createdAt(at)
            .updatedAt(at)
            .build());
  }
}
