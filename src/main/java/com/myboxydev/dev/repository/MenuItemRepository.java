package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.MenuItemModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItemModel, UUID> {
  List<MenuItemModel> findByIsActiveTrueOrderByDisplayOrderAsc();
}