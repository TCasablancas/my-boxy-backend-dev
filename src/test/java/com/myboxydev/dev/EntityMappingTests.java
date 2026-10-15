package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EntityMappingTests {
  @Autowired
  private EntityManagerFactory entityManagerFactory;

  // Sem hibernate-spatial o Point vira VARBINARY e o insert em GEOGRAPHY falha no Postgres
  @Test
  void addressLocationIsMappedAsSpatialType() {
    entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel()
            .getEntityDescriptor(UserAddressEntity.class)
            .findAttributeMapping("location")
            .forEachSelectable((index, selectable) ->
                    assertThat(selectable.getJdbcMapping().getJdbcType()).isNotInstanceOf(VarbinaryJdbcType.class));
  }

  // Colunas NOT NULL de public.user_profiles no Supabase. O H2 dos testes nasce da
  // entidade: se ela aceitar null onde o banco não aceita, o cadastro só quebra em produção.
  @Test
  void userProfileRequiredColumnsMatchSupabase() {
    var descriptor = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel()
            .getEntityDescriptor(UserProfileEntity.class);

    for (String attribute : List.of("fullName", "alias", "cpfEncrypted", "cpfHash", "email", "isSeller",
            "status", "createdAt", "updatedAt", "hasCpfRegistered", "hasStoreRegistered")) {
      descriptor.findAttributeMapping(attribute).forEachSelectable((index, selectable) ->
              assertThat(selectable.isNullable()).as(selectable.getSelectionExpression()).isFalse());
    }
  }
}
