package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.SellerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfileEntity, UUID> {
  Optional<SellerProfileEntity> findByUserProfileId(UUID userId);
  Optional<SellerProfileEntity> findBySlug(String slug);
  boolean existsBySlugIgnoreCase(String slug);
}
