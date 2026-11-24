package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.ProductQuestionEntity;
import com.myboxydev.dev.model.ProductModel;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductModel, UUID>, JpaSpecificationExecutor<ProductModel> {
    List<ProductModel> findByStoreIdAndIdNot(UUID storeId, UUID productId);
    List<ProductModel> findByNameContainingIgnoreCase(String name);

    // "Parecidos": sorteio de produtos da mesma subcategoria, sem o atual.
    // O sorteio e o limite ficam no banco para não carregar a subcategoria inteira.
    @Query(value = """
            SELECT * FROM products
            WHERE subcategory_id = :subcategoryId AND id <> :productId
            ORDER BY random()
            LIMIT :limit
            """, nativeQuery = true)
    List<ProductModel> findRandomBySubcategory(@Param("subcategoryId") UUID subcategoryId,
                                               @Param("productId") UUID productId,
                                               @Param("limit") int limit);

    // Favoritos por usuário (tabela product_favorites). Insert/delete idempotentes.
    @Modifying
    @Query(value = """
            INSERT INTO product_favorites (user_id, product_id) VALUES (:userId, :productId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int addFavorite(@Param("userId") UUID userId, @Param("productId") UUID productId);

    @Modifying
    @Query(value = "DELETE FROM product_favorites WHERE user_id = :userId AND product_id = :productId",
            nativeQuery = true)
    int removeFavorite(@Param("userId") UUID userId, @Param("productId") UUID productId);

    @Query(value = "SELECT product_id FROM product_favorites WHERE user_id = :userId AND product_id IN (:ids)",
            nativeQuery = true)
    List<UUID> findFavoritedIds(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    // Perguntas do produto (tabela product_questions). Paginação por cursor
    // (created_at, id), mais recentes primeiro, igual à lista de avaliações.
    @Query("""
            SELECT q FROM ProductQuestionEntity q
            WHERE q.productId = :productId
              AND (q.createdAt < :cursorAt OR (q.createdAt = :cursorAt AND q.id < :cursorId))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQuestionEntity> findQuestionsPage(@Param("productId") UUID productId,
                                                  @Param("cursorAt") OffsetDateTime cursorAt,
                                                  @Param("cursorId") UUID cursorId,
                                                  Limit limit);
}
