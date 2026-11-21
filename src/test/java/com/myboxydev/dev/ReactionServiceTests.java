package com.myboxydev.dev;

import com.myboxydev.dev.dto.ToggleStateDTO;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.ReviewRepository;
import com.myboxydev.dev.service.ReactionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactionServiceTests {
  private final ProductRepository products = mock(ProductRepository.class);
  private final ReviewRepository reviews = mock(ReviewRepository.class);
  private final ReactionService service = new ReactionService(products, reviews);

  private final UUID userId = UUID.randomUUID();
  private final UUID productId = UUID.randomUUID();
  private final UUID reviewId = UUID.randomUUID();

  @Test
  void favoriteUnknownProductIs404AndWritesNothing() {
    when(products.existsById(productId)).thenReturn(false);

    assertThatThrownBy(() -> service.setProductFavorite(userId, productId, true))
            .isInstanceOf(ResourceNotFoundException.class);
    verify(products, never()).addFavorite(any(), any());
  }

  @Test
  void favoriteAndUnfavoriteWrite() {
    when(products.existsById(productId)).thenReturn(true);

    assertThat(service.setProductFavorite(userId, productId, true)).isEqualTo(new ToggleStateDTO(true, null));
    verify(products).addFavorite(userId, productId);
    assertThat(service.setProductFavorite(userId, productId, false).active()).isFalse();
    verify(products).removeFavorite(userId, productId);
  }

  @Test
  void likeReturnsCountFromDatabase() {
    when(reviews.findPublishedAuthorId(reviewId)).thenReturn(Optional.of(UUID.randomUUID()));
    when(reviews.findLikesCount(reviewId)).thenReturn(7);

    assertThat(service.setReviewLike(userId, reviewId, true)).isEqualTo(new ToggleStateDTO(true, 7L));
    verify(reviews).addLike(reviewId, userId);
  }

  @Test
  void cannotLikeOwnReview() {
    when(reviews.findPublishedAuthorId(reviewId)).thenReturn(Optional.of(userId));

    assertThatThrownBy(() -> service.setReviewLike(userId, reviewId, true))
            .isInstanceOf(BusinessRuleException.class);
    verify(reviews, never()).addLike(any(), any());
  }

  @Test
  void likeOnUnpublishedReviewIs404() {
    when(reviews.findPublishedAuthorId(reviewId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setReviewLike(userId, reviewId, false))
            .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void emptyIdsSkipQueryAndTooManyIs400() {
    assertThat(service.likedReviewIds(userId, List.of())).isEmpty();
    verify(reviews, never()).findLikedIds(any(), any());

    List<UUID> tooMany = Collections.nCopies(ReactionService.MAX_IDS + 1, productId);
    assertThatThrownBy(() -> service.favoritedProductIds(userId, tooMany))
            .isInstanceOf(FieldValidationException.class);
  }
}
