package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.HomeBannerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HomeBannerRepository extends JpaRepository<HomeBannerEntity, UUID> {
  // Mesmo filtro da policy de RLS: o BFF lê com o usuário do banco, que ignora RLS
  @Query("""
          SELECT b FROM HomeBannerEntity b
          WHERE b.isActive = true
            AND (b.startsAt IS NULL OR b.startsAt <= :now)
            AND (b.endsAt IS NULL OR b.endsAt > :now)
          ORDER BY b.sortOrder ASC, b.createdAt DESC
          """)
  List<HomeBannerEntity> findLive(@Param("now") OffsetDateTime now, Pageable page);
}
