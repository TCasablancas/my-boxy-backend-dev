package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.ProductMediaEntity;
import com.myboxydev.dev.domain.enums.ProductMediaType;
import com.myboxydev.dev.model.CategoryModel;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
import com.myboxydev.dev.model.SubcategoryModel;
import com.myboxydev.dev.repository.ProductMediaRepository;
import com.myboxydev.dev.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProductRepository productRepository;

  @MockitoBean
  private ProductMediaRepository productMediaRepository;

  private final UUID productId = UUID.randomUUID();

  @Test
  void detailReturnsMediaInCarouselOrder() throws Exception {
    when(productRepository.findById(productId)).thenReturn(Optional.of(product()));
    when(productMediaRepository.findByProductIdOrderByPositionAsc(productId)).thenReturn(List.of(
            media(ProductMediaType.IMAGE, "https://cdn.test/1.webp", null, 0),
            media(ProductMediaType.VIDEO, "https://cdn.test/2.mp4", "https://cdn.test/2.webp", 1)));

    mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mainImage").value("https://cdn.test/capa.webp"))
            .andExpect(jsonPath("$.media.length()").value(2))
            .andExpect(jsonPath("$.media[0].type").value("IMAGE"))
            .andExpect(jsonPath("$.media[1].type").value("VIDEO"))
            .andExpect(jsonPath("$.media[1].url").value("https://cdn.test/2.mp4"))
            .andExpect(jsonPath("$.media[1].thumbnailUrl").value("https://cdn.test/2.webp"));
  }

  @Test
  void detailFallsBackToMainImageWithoutMedia() throws Exception {
    when(productRepository.findById(productId)).thenReturn(Optional.of(product()));
    when(productMediaRepository.findByProductIdOrderByPositionAsc(productId)).thenReturn(List.of());

    mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.media.length()").value(1))
            .andExpect(jsonPath("$.media[0].type").value("IMAGE"))
            .andExpect(jsonPath("$.media[0].url").value("https://cdn.test/capa.webp"))
            .andExpect(jsonPath("$.isCustom").value(false));
  }

  @Test
  void detailAndRelatedReturnIsCustom() throws Exception {
    ProductModel custom = product();
    custom.setIsCustom(true);
    when(productRepository.findById(productId)).thenReturn(Optional.of(custom));
    when(productRepository.findByStoreIdAndIdNot(custom.getStore().getId(), productId)).thenReturn(List.of(custom));

    mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isCustom").value(true));
    mockMvc.perform(get("/api/products/{id}/related", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isCustom").value(true));
  }

  @Test
  void detailReturnsSubcategoryWithCategoryName() throws Exception {
    CategoryModel category = new CategoryModel();
    category.setId(UUID.randomUUID());
    category.setName("Decoração");
    SubcategoryModel subcategory = new SubcategoryModel();
    subcategory.setId(UUID.randomUUID());
    subcategory.setName("Vasos");
    subcategory.setCategory(category);
    ProductModel withCategory = product();
    withCategory.setSubcategory(subcategory);
    when(productRepository.findById(productId)).thenReturn(Optional.of(withCategory));

    mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subcategory.name").value("Vasos"))
            .andExpect(jsonPath("$.subcategory.categoryId").value(category.getId().toString()))
            .andExpect(jsonPath("$.subcategory.categoryName").value("Decoração"));
  }

  @Test
  void similarReturnsRandomProductsFromSameSubcategory() throws Exception {
    SubcategoryModel subcategory = new SubcategoryModel();
    subcategory.setId(UUID.randomUUID());
    ProductModel current = product();
    current.setSubcategory(subcategory);
    ProductModel other = product();
    other.setId(UUID.randomUUID());
    other.setName("Vaso grande");
    when(productRepository.findById(productId)).thenReturn(Optional.of(current));
    when(productRepository.findRandomBySubcategory(subcategory.getId(), productId, 6)).thenReturn(List.of(other));

    mockMvc.perform(get("/api/products/{id}/similar", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(other.getId().toString()))
            .andExpect(jsonPath("$[0].name").value("Vaso grande"));
  }

  @Test
  void similarClampsLimit() throws Exception {
    SubcategoryModel subcategory = new SubcategoryModel();
    subcategory.setId(UUID.randomUUID());
    ProductModel current = product();
    current.setSubcategory(subcategory);
    when(productRepository.findById(productId)).thenReturn(Optional.of(current));

    mockMvc.perform(get("/api/products/{id}/similar", productId).param("limit", "500"))
            .andExpect(status().isOk());
    mockMvc.perform(get("/api/products/{id}/similar", productId).param("limit", "0"))
            .andExpect(status().isOk());

    verify(productRepository).findRandomBySubcategory(subcategory.getId(), productId, 20);
    verify(productRepository).findRandomBySubcategory(subcategory.getId(), productId, 1);
  }

  @Test
  void similarIsEmptyWithoutSubcategory() throws Exception {
    when(productRepository.findById(productId)).thenReturn(Optional.of(product()));

    mockMvc.perform(get("/api/products/{id}/similar", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    verify(productRepository, never()).findRandomBySubcategory(any(), any(), anyInt());
  }

  @Test
  void similarReturns404ForUnknownProduct() throws Exception {
    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/products/{id}/similar", productId)).andExpect(status().isNotFound());
  }

  @Test
  void detailReturns404ForUnknownProduct() throws Exception {
    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/products/{id}", productId)).andExpect(status().isNotFound());
  }

  private ProductModel product() {
    StoreModel store = new StoreModel();
    store.setId(UUID.randomUUID());
    store.setName("Ateliê");
    ProductModel p = new ProductModel();
    p.setId(productId);
    p.setName("Vaso");
    p.setPrice(new BigDecimal("99.90"));
    p.setMainImage("https://cdn.test/capa.webp");
    p.setStore(store);
    return p;
  }

  private ProductMediaEntity media(ProductMediaType type, String url, String thumbnail, int position) {
    return ProductMediaEntity.builder().productId(productId).mediaType(type).url(url)
            .thumbnailUrl(thumbnail).position((short) position).build();
  }
}
