package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.UserAddressEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
}
