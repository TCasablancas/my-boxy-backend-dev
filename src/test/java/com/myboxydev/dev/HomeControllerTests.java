package com.myboxydev.dev;

import com.myboxydev.dev.dto.HomeBannerDTO;
import com.myboxydev.dev.dto.HomeHeaderDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.HomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private HomeService homeService;

  @Test
  void bannersArePublicCachedAndSnakeCase() throws Exception {
    UUID id = UUID.randomUUID();
    when(homeService.getLiveBanners()).thenReturn(List.of(
            new HomeBannerDTO(id, "https://cdn.test/a.webp", "Promoção", "/search", null)));

    mockMvc.perform(get("/api/home/banners"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("max-age=300")))
            .andExpect(header().string("Cache-Control", containsString("public")))
            .andExpect(header().exists("ETag"))
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].image_url").value("https://cdn.test/a.webp"))
            .andExpect(jsonPath("$[0].alt_text").value("Promoção"))
            .andExpect(jsonPath("$[0].internal_route").value("/search"));
  }

  @Test
  void sameEtagGets304WithoutBody() throws Exception {
    when(homeService.getLiveBanners()).thenReturn(List.of());

    MvcResult first = mockMvc.perform(get("/api/home/banners")).andReturn();
    String etag = first.getResponse().getHeader("ETag");

    mockMvc.perform(get("/api/home/banners").header("If-None-Match", etag))
            .andExpect(status().isNotModified());
  }

  @Test
  void headerReturnsOnlyWhatTheHeaderShows() throws Exception {
    UUID userId = UUID.randomUUID();
    when(homeService.getHeader(userId)).thenReturn(
            new HomeHeaderDTO("Ana Souza", "ana.souza", new HomeHeaderDTO.Location("Santos", "SP")));

    mockMvc.perform(get("/api/v1/me/home-header").with(jwt().jwt(t -> t.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("private")))
            .andExpect(jsonPath("$.display_name").value("Ana Souza"))
            .andExpect(jsonPath("$.alias").value("ana.souza"))
            .andExpect(jsonPath("$.location.city").value("Santos"))
            .andExpect(jsonPath("$.location.state").value("SP"))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.cpf_masked").doesNotExist())
            .andExpect(jsonPath("$.phone_number").doesNotExist())
            .andExpect(jsonPath("$.location.street").doesNotExist());
  }

  @Test
  void headerWithoutTokenIs401() throws Exception {
    mockMvc.perform(get("/api/v1/me/home-header")).andExpect(status().isUnauthorized());
  }

  @Test
  void headerWithoutProfileIs404() throws Exception {
    when(homeService.getHeader(any())).thenThrow(new ResourceNotFoundException("Perfil não encontrado"));

    mockMvc.perform(get("/api/v1/me/home-header")
                    .with(jwt().jwt(t -> t.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isNotFound());
  }
}
