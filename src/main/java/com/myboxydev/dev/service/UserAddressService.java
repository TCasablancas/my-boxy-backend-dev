package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.domain.enums.AddressType;
import com.myboxydev.dev.dto.AddressRequestDTO;
import com.myboxydev.dev.dto.AddressResponseDTO;
import com.myboxydev.dev.dto.UserAddressDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.UserAddressModel;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAddressService {
  private final UserAddressRepository addressRepository;
  private final UserProfileRepository userProfileRepository;

  @Transactional(readOnly = true)
  public List<AddressResponseDTO> getUserAddresses(UUID userId) {
    return addressRepository.findByUserProfileIdOrderByIsPrimaryDescCreatedAtDesc(userId)
            .stream()
            .map(this::toResponseDTO)
            .toList();
  }

  @Transactional
  public AddressResponseDTO createAddress(UUID userId, AddressRequestDTO request) {
    UserProfileEntity user = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

    if (Boolean.TRUE.equals(request.isPrimary())) {
      addressRepository.clearPrimaryAddressesForUser(userId);
    }

    UserAddressEntity address = UserAddressEntity.builder()
            .userProfile(user)
            .title(request.title())
            .street(request.street())
            .number(request.number())
            .complement(request.complement())
            .neighborhood(request.neighborhood())
            .city(request.city())
            .state(request.state().toUpperCase())
            .postalCode(request.postalCode())
            .isPrimary(Boolean.TRUE.equals(request.isPrimary()))
            .addressType(AddressType.valueOf(request.addressType()))
            .build();

    addressRepository.save(address);
    return toResponseDTO(address);
  }

  @Transactional
  public AddressResponseDTO setPrimaryAddress(UUID userId, UUID addressId) {
    // Confere o dono antes de mexer em qualquer endereço; de outro usuário é 404
    UserAddressEntity address = addressRepository.findByIdAndUserProfileId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado ou não pertence a este usuário"));
    addressRepository.clearPrimaryAddressesForUser(userId);
    address.setIsPrimary(true);
    UserAddressEntity updated = addressRepository.save(address);
    return toResponseDTO(updated);
  }


  @Transactional
  public void deleteAddress(UUID userId, UUID addressId) {
    UserAddressEntity address = addressRepository.findByIdAndUserProfileId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado ou não pertence a este usuário"));
    addressRepository.delete(address);
  }

  private AddressResponseDTO toResponseDTO(UserAddressEntity entity) {
    return new AddressResponseDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getStreet(),
            entity.getNumber(),
            entity.getComplement(),
            entity.getNeighborhood(),
            entity.getCity(),
            entity.getState(),
            entity.getPostalCode(),
            entity.getIsPrimary(),
            entity.getAddressType().name()
    );
  }

//  private UserAddressDTO toDTO(UserAddressModel model) {
//    return new UserAddressDTO(
//            model.getId(),
//            model.getLabel(),
//            model.getStreet(),
//            model.getNumber(),
//            model.getComplement(),
//            model.getNeighborhood(),
//            model.getCity(),
//            model.getState(),
//            model.getPostalCode(),
//            model.getIsPrimary(),
//            model.getAddressType()
//    );
//  }
}
