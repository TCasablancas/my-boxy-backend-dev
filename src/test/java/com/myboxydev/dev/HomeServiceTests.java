package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.HomeBannerEntity;
import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.HomeBannerDTO;
import com.myboxydev.dev.dto.HomeHeaderDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.HomeBannerRepository;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.HomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeServiceTests {
  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

  private HomeBannerRepository bannerRepository;
  private UserProfileRepository profileRepository;
  private UserAddressRepository addressRepository;
  private HomeService service;

  @BeforeEach
  void setUp() {
    bannerRepository = mock(HomeBannerRepository.class);
    profileRepository = mock(UserProfileRepository.class);
    addressRepository = mock(UserAddressRepository.class);
    service = new HomeService(bannerRepository, profileRepository, addressRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void asksForAtMostEightLiveBannersAtTheCurrentTime() {
    when(bannerRepository.findLive(any(), any())).thenReturn(List.of());

    service.getLiveBanners();

    ArgumentCaptor<OffsetDateTime> now = ArgumentCaptor.forClass(OffsetDateTime.class);
    ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
    verify(bannerRepository).findLive(now.capture(), page.capture());
    assertThat(now.getValue().toInstant()).isEqualTo(NOW);
    assertThat(page.getValue().getPageSize()).isEqualTo(HomeService.MAX_BANNERS);
  }

  @Test
  void keepsValidLinksAndOrder() {
    when(bannerRepository.findLive(any(), any())).thenReturn(List.of(
            banner("https://cdn.test/a.webp", "/search", null),
            banner("https://cdn.test/b.webp", null, "https://example.com/promo"),
            banner("https://cdn.test/c.webp", null, null)));

    List<HomeBannerDTO> banners = service.getLiveBanners();

    assertThat(banners).extracting(HomeBannerDTO::imageUrl)
            .containsExactly("https://cdn.test/a.webp", "https://cdn.test/b.webp", "https://cdn.test/c.webp");
    assertThat(banners.get(0).internalRoute()).isEqualTo("/search");
    assertThat(banners.get(1).externalUrl()).isEqualTo("https://example.com/promo");
    assertThat(banners.get(2).internalRoute()).isNull();
    assertThat(banners.get(2).externalUrl()).isNull();
  }

  @Test
  void dangerousOrInvalidLinksAreDropped() {
    when(bannerRepository.findLive(any(), any())).thenReturn(List.of(
            banner("https://cdn.test/a.webp", null, "javascript:alert(1)"),
            banner("https://cdn.test/b.webp", null, "http://example.com"),
            banner("https://cdn.test/c.webp", null, "data:text/html;base64,AAAA"),
            banner("https://cdn.test/d.webp", "https://evil.test", null),
            banner("https://cdn.test/e.webp", "/search", "https://example.com")));

    List<HomeBannerDTO> banners = service.getLiveBanners();

    assertThat(banners).hasSize(5);
    assertThat(banners).allSatisfy(b -> {
      assertThat(b.internalRoute()).isNull();
      assertThat(b.externalUrl()).isNull();
    });
  }

  @Test
  void bannerWithoutHttpsImageIsLeftOut() {
    when(bannerRepository.findLive(any(), any())).thenReturn(List.of(
            banner("http://cdn.test/a.webp", null, null),
            banner("https://cdn.test/b.webp", null, null)));

    assertThat(service.getLiveBanners()).extracting(HomeBannerDTO::imageUrl)
            .containsExactly("https://cdn.test/b.webp");
  }

  @Test
  void headerHasOnlyNameAliasAndPrimaryCity() {
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.of(UserProfileEntity.builder()
            .id(userId).fullName("Ana Maria de Souza").alias("ana.souza").build()));
    when(addressRepository.findFirstByUserProfileIdAndIsPrimaryTrue(userId)).thenReturn(Optional.of(
            UserAddressEntity.builder().city("Santos").state("sp").street("Rua Teste").number("1").build()));

    HomeHeaderDTO header = service.getHeader(userId);

    assertThat(header.displayName()).isEqualTo("Ana Souza");
    assertThat(header.alias()).isEqualTo("ana.souza");
    assertThat(header.location()).isEqualTo(new HomeHeaderDTO.Location("Santos", "SP"));
  }

  @Test
  void headerWithoutPrimaryAddressHasNoLocation() {
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.of(UserProfileEntity.builder()
            .id(userId).fullName("Ana").alias("ana").build()));
    when(addressRepository.findFirstByUserProfileIdAndIsPrimaryTrue(userId)).thenReturn(Optional.empty());

    HomeHeaderDTO header = service.getHeader(userId);

    assertThat(header.displayName()).isEqualTo("Ana");
    assertThat(header.location()).isNull();
  }

  @Test
  void headerWithoutProfileIs404() {
    UUID userId = UUID.randomUUID();
    when(profileRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getHeader(userId)).isInstanceOf(ResourceNotFoundException.class);
  }

  private HomeBannerEntity banner(String imageUrl, String internalRoute, String externalUrl) {
    return HomeBannerEntity.builder()
            .id(UUID.randomUUID())
            .imageUrl(imageUrl)
            .altText("Banner")
            .internalRoute(internalRoute)
            .externalUrl(externalUrl)
            .isActive(true)
            .build();
  }
}
