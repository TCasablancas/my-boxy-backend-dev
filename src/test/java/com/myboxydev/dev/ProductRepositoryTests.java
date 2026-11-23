package com.myboxydev.dev;

import com.myboxydev.dev.model.CategoryModel;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.model.SubcategoryModel;
import com.myboxydev.dev.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// A query nativa de "Parecidos" roda de verdade no H2 (modo PostgreSQL).
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositoryTests {
  @Autowired
  private ProductRepository repository;

  @Autowired
  private EntityManager em;

  @Test
  void findRandomBySubcategoryReturnsSameSubcategoryWithoutCurrentProductUpToLimit() {
    CategoryModel category = new CategoryModel();
    category.setName("Casa");
    category.setSlug("casa");
    category.setSortOrder(0);
    em.persist(category);
    SubcategoryModel vasos = subcategory(category, "vasos");
    SubcategoryModel mesas = subcategory(category, "mesas");
    StoreModel store = new StoreModel();
    store.setName("Ateliê");
    store.setLocation("Santos");
    store.setImage("https://cdn.test/loja.webp");
    store.setRating(BigDecimal.ONE);
    em.persist(store);

    ProductModel current = product(store, vasos, "atual");
    product(store, vasos, "vaso-1");
    product(store, vasos, "vaso-2");
    product(store, vasos, "vaso-3");
    product(store, mesas, "mesa");
    em.flush();

    assertThat(repository.findRandomBySubcategory(vasos.getId(), current.getId(), 10))
            .extracting(ProductModel::getName)
            .containsExactlyInAnyOrder("vaso-1", "vaso-2", "vaso-3");
    assertThat(repository.findRandomBySubcategory(vasos.getId(), current.getId(), 2)).hasSize(2);
  }

  private SubcategoryModel subcategory(CategoryModel category, String slug) {
    SubcategoryModel subcategory = new SubcategoryModel();
    subcategory.setCategory(category);
    subcategory.setName(slug);
    subcategory.setSlug(slug);
    subcategory.setSortOrder(0);
    em.persist(subcategory);
    return subcategory;
  }

  private ProductModel product(StoreModel store, SubcategoryModel subcategory, String name) {
    ProductModel product = new ProductModel();
    product.setName(name);
    product.setPrice(BigDecimal.TEN);
    product.setMainImage("https://cdn.test/" + name + ".webp");
    product.setIsFavorite(false);
    product.setInStock(true);
    product.setNumSells(0);
    product.setDiscount(BigDecimal.ZERO);
    product.setReviews(0);
    product.setStore(store);
    product.setSubcategory(subcategory);
    em.persist(product);
    return product;
  }
}
