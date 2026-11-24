package com.myboxydev.dev;

import com.myboxydev.dev.dto.CartDTO;
import com.myboxydev.dev.dto.CartItemDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CartService cartService;

  private final UUID userId = UUID.randomUUID();
  private final UUID productId = UUID.randomUUID();

  @Test
  void cartRequiresToken() throws Exception {
    mockMvc.perform(get("/api/v1/me/cart")).andExpect(status().isUnauthorized());
    mockMvc.perform(put("/api/v1/me/cart/items/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
            .andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/v1/me/cart/items/{id}", productId)).andExpect(status().isUnauthorized());
    verify(cartService, never()).setQuantity(any(), any(), any());
  }

  @Test
  void getCartUsesTokenSubjectAndSnakeCase() throws Exception {
    when(cartService.getCart(userId)).thenReturn(new CartDTO(List.of(item(2))));

    mockMvc.perform(get("/api/v1/me/cart").with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].product_id").value(productId.toString()))
            .andExpect(jsonPath("$.items[0].unit_price").value(10))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].store_name").value("Ateliê"));
  }

  @Test
  void putSetsQuantity() throws Exception {
    when(cartService.setQuantity(userId, productId, 3)).thenReturn(item(3));

    mockMvc.perform(put("/api/v1/me/cart/items/{id}", productId).with(asUser())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(3));
    verify(cartService).setQuantity(userId, productId, 3);
  }

  @Test
  void putInvalidQuantityIs400AndUnknownProductIs404() throws Exception {
    when(cartService.setQuantity(userId, productId, 0))
            .thenThrow(new FieldValidationException("quantity", "A quantidade deve ficar entre 1 e 99"));
    mockMvc.perform(put("/api/v1/me/cart/items/{id}", productId).with(asUser())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
            .andExpect(status().isBadRequest());

    when(cartService.setQuantity(userId, productId, 1)).thenThrow(new ResourceNotFoundException("Produto não encontrado"));
    mockMvc.perform(put("/api/v1/me/cart/items/{id}", productId).with(asUser())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
            .andExpect(status().isNotFound());
  }

  @Test
  void deleteIs204() throws Exception {
    mockMvc.perform(delete("/api/v1/me/cart/items/{id}", productId).with(asUser()))
            .andExpect(status().isNoContent());
    verify(cartService).remove(userId, productId);
  }

  private CartItemDTO item(int quantity) {
    return new CartItemDTO(productId, "Vaso", BigDecimal.TEN, "https://cdn.test/vaso.webp", quantity, true,
            UUID.randomUUID(), "Ateliê", null);
  }

  private RequestPostProcessor asUser() {
    return jwt().jwt(token -> token.subject(userId.toString()));
  }
}
