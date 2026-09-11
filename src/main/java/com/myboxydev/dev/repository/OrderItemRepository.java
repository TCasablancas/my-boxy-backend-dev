package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.OrderItemModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemModel, UUID> {
  List<OrderItemModel> findByOrderId(UUID orderId);
}