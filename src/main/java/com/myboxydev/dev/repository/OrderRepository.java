package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, UUID>, JpaSpecificationExecutor<OrderModel> {

  Optional<OrderModel> findByOrderNumber(String orderNumber);

  List<OrderModel> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<OrderModel> findByGuestEmailOrderByCreatedAtDesc(String guestEmail);

  List<OrderModel> findByStoreIdOrderByCreatedAtDesc(UUID storeId);
}