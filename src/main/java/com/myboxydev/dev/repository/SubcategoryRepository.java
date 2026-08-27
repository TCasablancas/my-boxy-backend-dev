package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.SubcategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubcategoryRepository extends JpaRepository<SubcategoryModel, UUID> {
    List<SubcategoryModel> findAllByOrderBySortOrderAsc();
}
