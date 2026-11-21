package com.myboxydev.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.rate-limit.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterTests {

  @Autowired
  private MockMvc mockMvc;

  private void checkCpf(String forwardedFor, int expectedStatus) throws Exception {
    mockMvc.perform(post("/api/auth/signup/check-cpf")
                    .header("X-Forwarded-For", forwardedFor)
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().is(expectedStatus));
  }

  @Test
  void blocksCpfLookupAfterLimitWithRetryAfter() throws Exception {
    for (int i = 0; i < 10; i++) {
      checkCpf("203.0.113.10", 400);
    }
    mockMvc.perform(post("/api/auth/signup/check-cpf")
                    .header("X-Forwarded-For", "203.0.113.10")
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
            .andExpect(jsonPath("$.status").value(429));
  }

  @Test
  void limitIsPerClientIpUsingLastForwardedHop() throws Exception {
    for (int i = 0; i < 10; i++) {
      // O cliente inventa o primeiro IP; vale o último, que o proxy acrescenta
      checkCpf("1.1.1." + i + ", 203.0.113.20", 400);
    }
    checkCpf("9.9.9.9, 203.0.113.20", 429);
    checkCpf("203.0.113.21", 400);
  }

  @Test
  void doesNotLimitPublicCatalog() throws Exception {
    for (int i = 0; i < 40; i++) {
      mockMvc.perform(get("/api/products").header("X-Forwarded-For", "203.0.113.30"))
              .andExpect(status().isOk());
    }
  }
}
