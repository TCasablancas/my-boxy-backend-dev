package com.myboxydev.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Garante que o contrato OpenAPI usado pelo app Flutter continua sendo publicado
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void exposesOpenApiContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup']").exists());
  }

  @Test
  void documentsHomeEndpointsAndBearerScheme() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/home/banners'].get.operationId").value("getHomeBanners"))
            .andExpect(jsonPath("$.paths['/api/v1/me/home-header'].get.operationId").value("getMyHomeHeader"))
            .andExpect(jsonPath("$.paths['/api/v1/me/home-header'].get.security[0].bearerAuth").exists())
            .andExpect(jsonPath("$.components.schemas.HomeBannerDTO.properties.image_url").exists())
            .andExpect(jsonPath("$.components.schemas.HomeHeaderDTO.properties.display_name").exists())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
  }
}
