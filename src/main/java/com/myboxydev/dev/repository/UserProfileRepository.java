package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
  boolean existsByAliasIgnoreCase(String alias);
  boolean existsByEmailIgnoreCase(String email);
  boolean existsByCpfHash(String cpfHash);
  Optional<UserProfileEntity> findByAliasIgnoreCase(String alias);
  Optional<UserProfileEntity> findByEmailIgnoreCase(String email);
  Optional<UserProfileEntity> findByCpfHash(String cpfHash);
  // Consulta nativa para descriptografar CPF no banco via pgcrypto
  @Query(
    value = "SELECT pgp_sym_decrypt(cpf_encrypted\\:\\:bytea, :secretKey) FROM public.user_profiles WHERE id = :userId",
    nativeQuery = true
  )
  String decryptCpfByUserId(
    @Param("userId") UUID userId,
    @Param("secretKey") String secretKey
  );
  // Atualização atômica do CPF criptografado e do hash SHA-256
  @Query(
    value = "UPDATE public.user_profiles SET cpf_encrypted = pgp_sym_encrypt(:rawCpf, :secretKey), cpf_hash = :cpfHash, updated_at = NOW() WHERE id = :userId",
    nativeQuery = true
  )
  void updateCpfEncryptedAndHash(
    @Param("userId") UUID userId,
    @Param("rawCpf") String rawCpf,
    @Param("cpfHash") String cpfHash,
    @Param("secretKey") String secretKey
  );
}
