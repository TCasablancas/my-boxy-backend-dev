package com.myboxydev.dev;

import com.myboxydev.dev.domain.entity.ProductMediaEntity;
import com.myboxydev.dev.domain.enums.ProductMediaType;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.model.StoreModel;
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
