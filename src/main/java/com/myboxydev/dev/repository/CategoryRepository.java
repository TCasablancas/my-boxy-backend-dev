package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.CategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryModel, UUID> {
    List<CategoryModel> findAllByOrderBySortOrderAsc();
}
