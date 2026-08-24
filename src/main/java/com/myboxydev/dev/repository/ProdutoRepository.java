package com.myboxydev.dev.repository;

import com.myboxydev.dev.model.ProdutoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<ProdutoModel, UUID> {
    List<ProdutoModel> findByLojaIdAndIdNot(UUID lojaId, UUID produtoId);
    List<ProdutoModel> findByNameContainingIgnoreCase(String name);
}
