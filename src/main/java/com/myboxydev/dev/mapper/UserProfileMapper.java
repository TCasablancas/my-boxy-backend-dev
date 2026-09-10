package com.myboxydev.dev.mapper;

import com.myboxydev.dev.domain.entity.SellerProfileEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.SellerSummaryDTO;
import com.myboxydev.dev.dto.UserProfileResponseDTO;
import com.myboxydev.dev.util.CpfUtils;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
  public UserProfileResponseDTO toResponseDTO(UserProfileEntity entity, String decryptedCpf) {
    SellerSummaryDTO sellerSummary = null;

    if (Boolean.TRUE.equals(entity.getIsSeller()) && entity.getSellerProfile() != null) {
      SellerProfileEntity seller = entity.getSellerProfile();
      sellerSummary = new SellerSummaryDTO(
              seller.getId(),
              seller.getStoreName(),
              seller.getSlug(),
              seller.getIsFounder(),
              seller.getStripeOnboardingCompleted(),
              seller.getPickupEnabled()
      );
    }

    String maskedCpf = decryptedCpf != null ? CpfUtils.maskCpf(decryptedCpf) : "***.***.***-**";

    return new UserProfileResponseDTO(
            entity.getId(),
            entity.getFullName(),
            entity.getAlias(),
            entity.getEmail(),
            maskedCpf,
            entity.getHasCpfRegistered(),
            entity.getPhoneNumber(),
            entity.getAvatarUrl(),
            entity.getIsSeller(),
            entity.getHasStoreRegistered(),
            entity.getStripeCustomerId(),
            sellerSummary
    );
  }
}
