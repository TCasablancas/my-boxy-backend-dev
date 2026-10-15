package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.HomeBannerEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.HomeBannerDTO;
import com.myboxydev.dev.dto.HomeHeaderDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.HomeBannerRepository;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {
  /** O carrossel do app mostra no máximo 8 cards. */
  public static final int MAX_BANNERS = 8;

  // Mesmas regras dos CHECKs da tabela (ADR-0002), repetidas aqui como segunda barreira
  private static final Pattern HTTPS_URL = Pattern.compile("^https://\\S+$");
  private static final Pattern INTERNAL_ROUTE = Pattern.compile("^/[a-z0-9/_-]{0,199}$");

  private final HomeBannerRepository homeBannerRepository;
  private final UserProfileRepository userProfileRepository;
  private final UserAddressRepository userAddressRepository;
  private final Clock clock;

  /**
   * Banners no ar, na ordem do carrossel. Imagem fora da regra tira o banner;
   * link fora da regra deixa o banner sem link. Os dois casos vão para o log.
   */
  @Transactional(readOnly = true)
  public List<HomeBannerDTO> getLiveBanners() {
    List<HomeBannerEntity> banners = homeBannerRepository.findLive(
            OffsetDateTime.now(clock), PageRequest.of(0, MAX_BANNERS));
    return banners.stream()
            .filter(this::hasValidImage)
            .map(this::toDto)
            .toList();
  }

  /** Nome, alias e cidade/UF do endereço principal do usuário logado. */
  @Transactional(readOnly = true)
  public HomeHeaderDTO getHeader(UUID userId) {
    UserProfileEntity profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));
    HomeHeaderDTO.Location location = userAddressRepository.findFirstByUserProfileIdAndIsPrimaryTrue(userId)
            .map(address -> new HomeHeaderDTO.Location(address.getCity(), address.getState().toUpperCase(Locale.ROOT)))
            .orElse(null);
    return new HomeHeaderDTO(displayName(profile.getFullName()), profile.getAlias(), location);
  }

  /** "Ana Maria de Souza" vira "Ana Souza"; um nome só fica como está. */
  static String displayName(String fullName) {
    String[] parts = fullName == null ? new String[0] : fullName.trim().split("\\s+");
    if (parts.length == 0 || parts[0].isEmpty()) return "";
    return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1];
  }

  private boolean hasValidImage(HomeBannerEntity banner) {
    if (banner.getImageUrl() != null && HTTPS_URL.matcher(banner.getImageUrl()).matches()) return true;
    log.warn("Banner {} fora do carrossel: image_url não é https", banner.getId());
    return false;
  }

  private HomeBannerDTO toDto(HomeBannerEntity banner) {
    String internalRoute = banner.getInternalRoute();
    String externalUrl = banner.getExternalUrl();
    if (internalRoute != null && !INTERNAL_ROUTE.matcher(internalRoute).matches()) {
      log.warn("Banner {} sem link: internal_route inválida", banner.getId());
      internalRoute = null;
    }
    if (externalUrl != null && !HTTPS_URL.matcher(externalUrl).matches()) {
      log.warn("Banner {} sem link: external_url não é https", banner.getId());
      externalUrl = null;
    }
    if (internalRoute != null && externalUrl != null) {
      log.warn("Banner {} sem link: tem rota interna e URL externa ao mesmo tempo", banner.getId());
      internalRoute = null;
      externalUrl = null;
    }
    return new HomeBannerDTO(banner.getId(), banner.getImageUrl(), banner.getAltText(), internalRoute, externalUrl);
  }
}
