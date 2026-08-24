package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.ProductModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductModel, UUID> {
    List<ProductModel> findByLojaIdAndIdNot(UUID lojaId, UUID produtoId);
    List<ProductModel> findByNameContainingIgnoreCase(String name);
}
