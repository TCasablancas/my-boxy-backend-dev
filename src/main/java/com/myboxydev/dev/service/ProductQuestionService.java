package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.ProductQuestionEntity;
import com.myboxydev.dev.domain.enums.UserStatus;
import com.myboxydev.dev.dto.ProductQuestionDTO;
import com.myboxydev.dev.dto.ProductQuestionPageDTO;
import com.myboxydev.dev.dto.ReviewDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.ReviewRepository;
import com.myboxydev.dev.util.ReviewCursor;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Perguntas públicas do produto: lista infinita (pública) e nova pergunta (usuário logado).
 * Reusa o cursor e o formato de autor da lista de avaliações.
 */
@Service
@RequiredArgsConstructor
public class ProductQuestionService {
  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 50;
  public static final int MAX_QUESTION_LENGTH = 500;

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;
  private final EntityManager entityManager;

  /**
   * @param cursor next_cursor da página anterior; null = primeira página
   * @param limit  itens por página (1 a 50); null = 20
   */
  @Transactional(readOnly = true)
  public ProductQuestionPageDTO list(UUID productId, String cursor, Integer limit) {
    int size = validateLimit(limit);
    ReviewCursor position = ReviewCursor.decode(cursor);
    ensureProductExists(productId);

    // Busca 1 a mais para saber se existe próxima página sem fazer COUNT
    List<ProductQuestionEntity> rows = productRepository.findQuestionsPage(
            productId, position.createdAt(), position.id(), Limit.of(size + 1));
    boolean hasMore = rows.size() > size;
    List<ProductQuestionEntity> page = hasMore ? rows.subList(0, size) : rows;

    Map<UUID, ReviewRepository.AuthorView> authors = findAuthors(
            page.stream().map(ProductQuestionEntity::getAuthorId).collect(Collectors.toSet()));
    List<ProductQuestionDTO> items = page.stream().map(q -> toDto(q, authors.get(q.getAuthorId()))).toList();

    String nextCursor = null;
    if (hasMore) {
      ProductQuestionEntity last = page.get(page.size() - 1);
      nextCursor = new ReviewCursor(last.getCreatedAt(), last.getId()).encode();
    }
    return new ProductQuestionPageDTO(items, nextCursor, hasMore);
  }

  @Transactional
  public ProductQuestionDTO ask(UUID userId, UUID productId, String question) {
    String text = validateQuestion(question);
    ensureProductExists(productId);

    // Microssegundos: mesma precisão do timestamptz, para o cursor bater com o banco
    ProductQuestionEntity created = ProductQuestionEntity.builder()
            .id(UUID.randomUUID())
            .productId(productId)
            .authorId(userId)
            .question(text)
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS))
            .build();
    // persist (e não insert nativo): o created_at passa pela mesma conversão de fuso
    // das leituras, senão o cursor da página seguinte não bate com o valor gravado.
    entityManager.persist(created);
    return toDto(created, findAuthors(Set.of(userId)).get(userId));
  }

  private Map<UUID, ReviewRepository.AuthorView> findAuthors(Set<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return reviewRepository.findAuthors(ids).stream()
            .collect(Collectors.toMap(ReviewRepository.AuthorView::getId, Function.identity()));
  }

  private ProductQuestionDTO toDto(ProductQuestionEntity q, ReviewRepository.AuthorView author) {
    ReviewDTO.Author authorDto = (author == null || author.getStatus() == UserStatus.ANONYMIZED)
            ? new ReviewDTO.Author(ReviewService.ANONYMOUS_NAME, null, null)
            : new ReviewDTO.Author(ReviewService.displayName(author.getFullName()), author.getAlias(), author.getAvatarUrl());
    return new ProductQuestionDTO(q.getId(), q.getQuestion(), q.getAnswer(), q.getCreatedAt(), q.getAnsweredAt(), authorDto);
  }

  private void ensureProductExists(UUID productId) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Produto não encontrado");
    }
  }

  static String validateQuestion(String question) {
    String text = question == null ? "" : question.strip();
    if (text.isEmpty()) {
      throw new FieldValidationException("question", "Escreva sua pergunta.");
    }
    if (text.codePointCount(0, text.length()) > MAX_QUESTION_LENGTH) {
      throw new FieldValidationException("question", "Use no máximo " + MAX_QUESTION_LENGTH + " caracteres.");
    }
    return text;
  }

  private static int validateLimit(Integer limit) {
    if (limit == null) return DEFAULT_LIMIT;
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new FieldValidationException("limit", "Use um limite entre 1 e " + MAX_LIMIT + ".");
    }
    return limit;
  }
}
