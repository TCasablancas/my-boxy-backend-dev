package com.myboxydev.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void permitsPublicCatalogEndpoints() throws Exception {
    mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk());
  }

  @Test
  void rejectsPrivateEndpointWithoutBearerToken() throws Exception {
    mockMvc.perform(get("/api/user_profiles/me"))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void permitsConfiguredCorsOrigin() throws Exception {
    mockMvc.perform(options("/api/products")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
  }
}
