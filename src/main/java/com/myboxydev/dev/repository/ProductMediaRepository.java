package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.ProductMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductMediaRepository extends JpaRepository<ProductMediaEntity, UUID> {
  List<ProductMediaEntity> findByProductIdOrderByPositionAsc(UUID productId);
}
