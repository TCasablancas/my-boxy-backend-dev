package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.ToggleStateDTO;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ações liga/desliga do usuário logado: favoritar produto e curtir avaliação.
 * Ligar e desligar são idempotentes (repetir não muda nada nem dá erro), então o
 * app pode reenviar sem medo depois de uma falha de rede.
 */
@Service
@RequiredArgsConstructor
public class ReactionService {
  public static final int MAX_IDS = 50;
  static final String SELF_LIKE_MESSAGE = "Não é possível curtir a própria avaliação";

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;

  @Transactional
  public ToggleStateDTO setProductFavorite(UUID userId, UUID productId, boolean active) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Produto não encontrado");
    }
    if (active) {
      productRepository.addFavorite(userId, productId);
    } else {
      productRepository.removeFavorite(userId, productId);
    }
    return new ToggleStateDTO(active, null);
  }

  @Transactional(readOnly = true)
  public List<UUID> favoritedProductIds(UUID userId, List<UUID> ids) {
    if (validateIds(ids)) return List.of();
    return productRepository.findFavoritedIds(userId, ids);
  }

  @Transactional
  public ToggleStateDTO setReviewLike(UUID userId, UUID reviewId, boolean active) {
    UUID authorId = reviewRepository.findPublishedAuthorId(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));
    if (active) {
      if (authorId.equals(userId)) throw new BusinessRuleException(SELF_LIKE_MESSAGE);
      try {
        reviewRepository.addLike(reviewId, userId);
      } catch (DataIntegrityViolationException e) {
        // Rede de segurança: o trigger review_likes_block_self também barra.
        throw new BusinessRuleException(SELF_LIKE_MESSAGE);
      }
    } else {
      reviewRepository.removeLike(reviewId, userId);
    }
    Integer count = reviewRepository.findLikesCount(reviewId);
    return new ToggleStateDTO(active, count == null ? 0L : count.longValue());
  }

  @Transactional(readOnly = true)
  public List<UUID> likedReviewIds(UUID userId, List<UUID> ids) {
    if (validateIds(ids)) return List.of();
    return reviewRepository.findLikedIds(userId, ids);
  }

  /** @return true quando a lista está vazia (nada a consultar). */
  private static boolean validateIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) return true;
    if (ids.size() > MAX_IDS) {
      throw new FieldValidationException("ids", "Envie no máximo " + MAX_IDS + " ids");
    }
    return false;
  }
}
