package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.ReviewEntity;
import com.myboxydev.dev.domain.enums.UserStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Leituras das avaliações (ADR-0003). Cada consulta usa um dos índices parciais
 * "status = PUBLISHED" da migration. A paginação é por cursor (created_at, id):
 * a primeira página usa um cursor sentinela no futuro, para não precisar de
 * parâmetro nulo.
 */
@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

  interface RatingCount {
    Short getRating();
    Long getTotal();
  }

  interface AuthorView {
    UUID getId();
    String getFullName();
    String getAlias();
    String getAvatarUrl();
    UserStatus getStatus();
  }

  @Query("""
          SELECT r.rating AS rating, COUNT(r) AS total FROM ReviewEntity r
          WHERE r.productId = :productId
            AND r.status = com.myboxydev.dev.domain.enums.ReviewStatus.PUBLISHED
          GROUP BY r.rating
          """)
  List<RatingCount> countByRatingForProduct(@Param("productId") UUID productId);

  @Query("""
          SELECT r.rating AS rating, COUNT(r) AS total FROM ReviewEntity r
          WHERE r.storeId = :storeId
            AND r.status = com.myboxydev.dev.domain.enums.ReviewStatus.PUBLISHED
          GROUP BY r.rating
          """)
  List<RatingCount> countByRatingForStore(@Param("storeId") UUID storeId);

  @Query("""
          SELECT r FROM ReviewEntity r
          WHERE r.productId = :productId
            AND r.status = com.myboxydev.dev.domain.enums.ReviewStatus.PUBLISHED
            AND r.rating IN :ratings
            AND (r.createdAt < :cursorAt OR (r.createdAt = :cursorAt AND r.id < :cursorId))
          ORDER BY r.createdAt DESC, r.id DESC
          """)
  List<ReviewEntity> findPageForProduct(@Param("productId") UUID productId,
                                        @Param("ratings") Collection<Short> ratings,
                                        @Param("cursorAt") OffsetDateTime cursorAt,
                                        @Param("cursorId") UUID cursorId,
                                        Limit limit);

  @Query("""
          SELECT r FROM ReviewEntity r
          WHERE r.storeId = :storeId
            AND r.status = com.myboxydev.dev.domain.enums.ReviewStatus.PUBLISHED
            AND r.rating IN :ratings
            AND (r.createdAt < :cursorAt OR (r.createdAt = :cursorAt AND r.id < :cursorId))
          ORDER BY r.createdAt DESC, r.id DESC
          """)
  List<ReviewEntity> findPageForStore(@Param("storeId") UUID storeId,
                                      @Param("ratings") Collection<Short> ratings,
                                      @Param("cursorAt") OffsetDateTime cursorAt,
                                      @Param("cursorId") UUID cursorId,
                                      Limit limit);

  /** Autor de uma avaliação publicada; vazio se ela não existe ou não está publicada. */
  @Query("""
          SELECT r.authorId FROM ReviewEntity r
          WHERE r.id = :reviewId
            AND r.status = com.myboxydev.dev.domain.enums.ReviewStatus.PUBLISHED
          """)
  java.util.Optional<UUID> findPublishedAuthorId(@Param("reviewId") UUID reviewId);

  // Curtidas (tabela review_likes). O trigger da migration mantém reviews.likes_count
  // e bloqueia curtir a própria avaliação. Insert/delete idempotentes.
  @Modifying
  @Query(value = """
          INSERT INTO review_likes (review_id, user_id) VALUES (:reviewId, :userId)
          ON CONFLICT DO NOTHING
          """, nativeQuery = true)
  int addLike(@Param("reviewId") UUID reviewId, @Param("userId") UUID userId);

  @Modifying
  @Query(value = "DELETE FROM review_likes WHERE review_id = :reviewId AND user_id = :userId",
          nativeQuery = true)
  int removeLike(@Param("reviewId") UUID reviewId, @Param("userId") UUID userId);

  @Query(value = "SELECT likes_count FROM reviews WHERE id = :reviewId", nativeQuery = true)
  Integer findLikesCount(@Param("reviewId") UUID reviewId);

  @Query(value = "SELECT review_id FROM review_likes WHERE user_id = :userId AND review_id IN (:ids)",
          nativeQuery = true)
  List<UUID> findLikedIds(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

  /** Só as colunas que o card mostra; nunca e-mail, CPF ou telefone. */
  @Query("""
          SELECT u.id AS id, u.fullName AS fullName, u.alias AS alias,
                 u.avatarUrl AS avatarUrl, u.status AS status
          FROM UserProfileEntity u WHERE u.id IN :ids
          """)
  List<AuthorView> findAuthors(@Param("ids") Collection<UUID> ids);
}
