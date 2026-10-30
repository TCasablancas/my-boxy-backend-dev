package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, UUID> {
  List<UserAddressEntity> findByUserProfileIdOrderByIsPrimaryDescCreatedAtDesc(UUID userId);

  Optional<UserAddressEntity> findByIdAndUserProfileId(UUID id, UUID userId);

  Optional<UserAddressEntity> findFirstByUserProfileIdAndIsPrimaryTrue(UUID userId);

  // Remove a flag de endereço principal de todos os endereços do usuário antes de definir um novo
  @Modifying
  @Query(
    "UPDATE UserAddressEntity a SET a.isPrimary = false WHERE a.userProfile.id = :userId"
  )
  void clearPrimaryAddressesForUser(
    @Param("userId") UUID userId
  );
}
