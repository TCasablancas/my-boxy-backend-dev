package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.domain.enums.AddressType;
import com.myboxydev.dev.dto.AddressRequestDTO;
import com.myboxydev.dev.dto.AddressResponseDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAddressService {
  private final UserAddressRepository addressRepository;
  private final UserProfileRepository userProfileRepository;
  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

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
      addressRepository.clearPrimaryAddressForUser(userId);
    }

    Point locationPoint = null;
    if (request.latitude() != null && request.longitude() != null) {
      // Nota PostGIS: Longitude (X), Latitude (Y)
      locationPoint = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
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
            .location(locationPoint)
            .build();

    addressRepository.save(address);
    return toResponseDTO(address);
  }

  @Transactional
  public void deleteAddress(UUID userId, UUID addressId) {
    UserAddressEntity address = addressRepository.findByIdAndUserProfileId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado ou não pertence a este usuário"));
    addressRepository.delete(address);
  }

  private AddressResponseDTO toResponseDTO(UserAddressEntity entity) {
    Double lat = entity.getLocation() != null ? entity.getLocation().getY() : null;
    Double lng = entity.getLocation() != null ? entity.getLocation().getX() : null;

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
            entity.getAddressType().name(),
            lat,
            lng
    );
  }
}
