package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartItemEntity, CartItemEntity.Key> {
  /** Itens do usuário, adicionados por último primeiro. */
  List<CartItemEntity> findByUserIdOrderByCreatedAtDescProductIdDesc(UUID userId);

  @Modifying
  @Query("DELETE FROM CartItemEntity c WHERE c.userId = :userId AND c.productId = :productId")
  int deleteItem(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
