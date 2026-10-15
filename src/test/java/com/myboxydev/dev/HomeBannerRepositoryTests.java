package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.HomeBannerEntity;
import com.myboxydev.dev.repository.HomeBannerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

// O filtro da query repete a policy de RLS (o BFF lê com o usuário do banco)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeBannerRepositoryTests {
  private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC);

  @Autowired
  private HomeBannerRepository repository;

  @Test
  void returnsOnlyActiveBannersInsideTheWindowOrderedBySortOrder() {
    save("ativo-2", true, 2, null, null);
    save("ativo-1", true, 1, NOW.minusDays(1), NOW.plusDays(1));
    save("inativo", false, 0, null, null);
    save("ainda-nao", true, 0, NOW.plusHours(1), null);
    save("ja-acabou", true, 0, null, NOW);

    assertThat(repository.findLive(NOW, PageRequest.of(0, 8)))
            .extracting(HomeBannerEntity::getAltText)
            .containsExactly("ativo-1", "ativo-2");
  }

  @Test
  void respectsTheLimit() {
    for (int i = 0; i < 10; i++) save("b" + i, true, i, null, null);

    assertThat(repository.findLive(NOW, PageRequest.of(0, 8))).hasSize(8);
  }

  private void save(String alt, boolean active, int order, OffsetDateTime startsAt, OffsetDateTime endsAt) {
    repository.saveAndFlush(HomeBannerEntity.builder()
            .imageUrl("https://cdn.test/" + alt + ".webp")
            .altText(alt)
            .isActive(active)
            .sortOrder(order)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build());
  }
}
