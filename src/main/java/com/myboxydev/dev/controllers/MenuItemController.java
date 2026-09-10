package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.MenuItemDTO;
import com.myboxydev.dev.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuItemController {

  @Autowired
  private MenuItemRepository menuItemRepository;

  @GetMapping
  public ResponseEntity<List<MenuItemDTO>> getMenuItems() {
    List<MenuItemDTO> items = menuItemRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
      .map(item -> new MenuItemDTO(
        item.getId(),
        item.getDescription(),
        item.getIconKey(),
        item.getActionIconKey(),
        item.getTargetRoute(),
        item.getDisplayOrder(),
        item.getRequiresAuth(),
        item.getRequiresSeller(),
        item.getBadgeCount()
      ))
      .collect(Collectors.toList());

    return ResponseEntity.ok(items);
  }
}