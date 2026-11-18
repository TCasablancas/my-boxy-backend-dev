package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.ReviewEntity;
import com.myboxydev.dev.domain.enums.ReviewTarget;
import com.myboxydev.dev.domain.enums.UserStatus;
import com.myboxydev.dev.dto.ReviewDTO;
import com.myboxydev.dev.dto.ReviewPageDTO;
import com.myboxydev.dev.dto.ReviewSummaryDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.ReviewRepository;
import com.myboxydev.dev.repository.StoreRepository;
import com.myboxydev.dev.util.ReviewCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Leitura das avaliações de produto e de loja (ADR-0003). O mesmo serviço
 * atende os dois alvos; o controller só escolhe o {@link ReviewTarget}.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 50;
  static final String ANONYMOUS_NAME = "Usuário MyBoxy";
  private static final List<Short> ALL_RATINGS = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);

  private final ReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;

  @Transactional(readOnly = true)
  public ReviewSummaryDTO getSummary(ReviewTarget target, UUID targetId) {
    ensureTargetExists(target, targetId);
    List<ReviewRepository.RatingCount> counts = target == ReviewTarget.PRODUCT
            ? reviewRepository.countByRatingForProduct(targetId)
            : reviewRepository.countByRatingForStore(targetId);

    long[] perRating = new long[6];
    for (ReviewRepository.RatingCount c : counts) {
      int rating = c.getRating();
      if (rating >= 1 && rating <= 5) perRating[rating] += c.getTotal();
    }
    long total = 0;
    long weighted = 0;
    for (int r = 1; r <= 5; r++) {
      total += perRating[r];
      weighted += perRating[r] * r;
    }

    List<ReviewSummaryDTO.RatingBucket> distribution = new ArrayList<>(5);
    for (int r = 5; r >= 1; r--) {
      double pct = total == 0 ? 0.0 : round1(perRating[r] * 100.0 / total);
      distribution.add(new ReviewSummaryDTO.RatingBucket(r, perRating[r], pct));
    }
    Double average = total == 0 ? null : round1((double) weighted / total);
    return new ReviewSummaryDTO(average, total, distribution);
  }

  /**
   * @param ratingsParam notas separadas por vírgula ("5,4"); null ou vazio = todas
   * @param cursor       next_cursor da página anterior; null = primeira página
   * @param limit        itens por página (1 a 50); null = 20
   */
  @Transactional(readOnly = true)
  public ReviewPageDTO list(ReviewTarget target, UUID targetId, String ratingsParam, String cursor, Integer limit) {
    List<Short> ratings = parseRatings(ratingsParam);
    int size = validateLimit(limit);
    ReviewCursor position = ReviewCursor.decode(cursor);
    ensureTargetExists(target, targetId);

    // Busca 1 a mais para saber se existe próxima página sem fazer COUNT
    Limit fetch = Limit.of(size + 1);
    List<ReviewEntity> rows = target == ReviewTarget.PRODUCT
            ? reviewRepository.findPageForProduct(targetId, ratings, position.createdAt(), position.id(), fetch)
            : reviewRepository.findPageForStore(targetId, ratings, position.createdAt(), position.id(), fetch);

    boolean hasMore = rows.size() > size;
    List<ReviewEntity> page = hasMore ? rows.subList(0, size) : rows;

    Map<UUID, ReviewRepository.AuthorView> authors = page.isEmpty() ? Map.of()
            : reviewRepository.findAuthors(page.stream().map(ReviewEntity::getAuthorId).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(ReviewRepository.AuthorView::getId, Function.identity()));

    List<ReviewDTO> items = page.stream().map(r -> toDto(r, authors.get(r.getAuthorId()))).toList();
    String nextCursor = null;
    if (hasMore) {
      ReviewEntity last = page.get(page.size() - 1);
      nextCursor = new ReviewCursor(last.getCreatedAt(), last.getId()).encode();
    }
    return new ReviewPageDTO(items, nextCursor, hasMore);
  }

  /** "Ana Maria de Souza" vira "Ana S."; um nome só fica como está. */
  static String displayName(String fullName) {
    String[] parts = fullName == null ? new String[0] : fullName.trim().split("\\s+");
    if (parts.length == 0 || parts[0].isEmpty()) return ANONYMOUS_NAME;
    if (parts.length == 1) return parts[0];
    String last = parts[parts.length - 1];
    return parts[0] + " " + last.substring(0, last.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT) + ".";
  }

  private ReviewDTO toDto(ReviewEntity r, ReviewRepository.AuthorView author) {
    ReviewDTO.Author authorDto = (author == null || author.getStatus() == UserStatus.ANONYMIZED)
            ? new ReviewDTO.Author(ANONYMOUS_NAME, null, null)
            : new ReviewDTO.Author(displayName(author.getFullName()), author.getAlias(), author.getAvatarUrl());
    return new ReviewDTO(
            r.getId(),
            r.getRating(),
            r.getComment(),
            r.getCreatedAt(),
            r.getEditedAt() != null,
            r.getOrderItemId() != null,
            r.getLikesCount() == null ? 0 : r.getLikesCount(),
            r.getPhotosCount() == null ? 0 : r.getPhotosCount(),
            authorDto);
  }

  private void ensureTargetExists(ReviewTarget target, UUID targetId) {
    boolean exists = target == ReviewTarget.PRODUCT
            ? productRepository.existsById(targetId)
            : storeRepository.existsById(targetId);
    if (!exists) {
      throw new ResourceNotFoundException(target == ReviewTarget.PRODUCT ? "Produto não encontrado" : "Loja não encontrada");
    }
  }

  static List<Short> parseRatings(String value) {
    if (value == null || value.isBlank()) return ALL_RATINGS;
    TreeSet<Short> ratings = new TreeSet<>();
    for (String part : value.split(",")) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) continue;
      if (!trimmed.matches("[1-5]")) {
        throw new FieldValidationException("ratings", "Use notas de 1 a 5 separadas por vírgula (ex.: 5,4).");
      }
      ratings.add(Short.parseShort(trimmed));
    }
    return ratings.isEmpty() ? ALL_RATINGS : List.copyOf(ratings);
  }

  private static int validateLimit(Integer limit) {
    if (limit == null) return DEFAULT_LIMIT;
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new FieldValidationException("limit", "Use um limite entre 1 e " + MAX_LIMIT + ".");
    }
    return limit;
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
