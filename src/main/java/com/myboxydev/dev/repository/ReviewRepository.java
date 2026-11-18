package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.ReviewEntity;
import com.myboxydev.dev.domain.enums.UserStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
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

  /** Só as colunas que o card mostra; nunca e-mail, CPF ou telefone. */
  @Query("""
          SELECT u.id AS id, u.fullName AS fullName, u.alias AS alias,
                 u.avatarUrl AS avatarUrl, u.status AS status
          FROM UserProfileEntity u WHERE u.id IN :ids
          """)
  List<AuthorView> findAuthors(@Param("ids") Collection<UUID> ids);
}
