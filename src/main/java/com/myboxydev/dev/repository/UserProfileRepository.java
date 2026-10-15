package com.myboxydev.dev.repository;

import com.myboxydev.dev.domain.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
  // Id do usuário no Supabase Auth (schema auth) para diagnosticar contas órfãs no cadastro
  @Query(
    value = "SELECT id FROM auth.users WHERE lower(email) = lower(:email) LIMIT 1",
    nativeQuery = true
  )
  UUID findAuthUserIdByEmail(@Param("email") String email);
  // Usuário criado agora há pouco no Auth e ainda sem perfil: resto de um cadastro
  // em que o Auth não respondeu a tempo (o relógio é o do banco)
  @Query(
    value = "SELECT u.id FROM auth.users u WHERE lower(u.email) = lower(:email) "
          + "AND u.created_at >= now() - interval '5 minutes' "
          + "AND NOT EXISTS (SELECT 1 FROM public.user_profiles p WHERE p.id = u.id) LIMIT 1",
    nativeQuery = true
  )
  UUID findRecentAuthUserIdWithoutProfile(@Param("email") String email);
  // Cifra o CPF com pgcrypto sem gravar nada, para o INSERT do perfil já levar o valor
  @Query(
    value = "SELECT CAST(extensions.pgp_sym_encrypt(:rawCpf, :secretKey) AS text)",
    nativeQuery = true
  )
  String encryptCpf(@Param("rawCpf") String rawCpf, @Param("secretKey") String secretKey);
  // Consulta nativa para descriptografar CPF no banco via pgcrypto (schema extensions no Supabase)
  @Query(
    value = "SELECT extensions.pgp_sym_decrypt(cpf_encrypted\\:\\:bytea, :secretKey) FROM public.user_profiles WHERE id = :userId",
    nativeQuery = true
  )
  String decryptCpfByUserId(
    @Param("userId") UUID userId,
    @Param("secretKey") String secretKey
  );
  // Atualização atômica do CPF criptografado e do hash SHA-256
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
    value = "UPDATE public.user_profiles SET cpf_encrypted = extensions.pgp_sym_encrypt(:rawCpf, :secretKey), cpf_hash = :cpfHash, updated_at = NOW() WHERE id = :userId",
    nativeQuery = true
  )
  void updateCpfEncryptedAndHash(
    @Param("userId") UUID userId,
    @Param("rawCpf") String rawCpf,
    @Param("cpfHash") String cpfHash,
    @Param("secretKey") String secretKey
  );
}
