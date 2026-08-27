package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.StoreModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<StoreModel, UUID> {
}
