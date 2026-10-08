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

  // Remove a flag de endereço principal de todos os endereços do usuário antes de definir um novo
  @Modifying
  @Query(
    "UPDATE UserAddressEntity a SET a.isPrimary = false WHERE a.userProfile.id = :userId"
  )
  void clearPrimaryAddressesForUser(
    @Param("userId") UUID userId
  );

  // Consulta espacial PostGIS: Verifica se dois endereços estão dentro de uma distância máxima em metros (Handshake/Geofencing)
  @Query(
    value = "SELECT ST_DWithin(a1.location, a2.location, :distanceInMeters) " +
            "FROM public.user_addresses a1, public.user_addresses a2 " +
            "WHERE a1.id = :buyerAddressId AND a2.id = :sellerAddressId", nativeQuery = true
  )
  Boolean isWithinHandshakeRadius(
    @Param("buyerAddressId") UUID buyerAddressId,
    @Param("sellerAddressId") UUID sellerAddressId,
    @Param("distanceInMeters") double distanceInMeters);
}
