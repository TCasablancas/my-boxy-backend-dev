package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.ProductModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductModel, UUID>, JpaSpecificationExecutor<ProductModel> {
    List<ProductModel> findByStoreIdAndIdNot(UUID storeId, UUID productId);
    List<ProductModel> findByNameContainingIgnoreCase(String name);
}
