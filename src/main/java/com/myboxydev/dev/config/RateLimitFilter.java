package com.myboxydev.dev.config;

import com.myboxydev.dev.dto.response.ApiErrorResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições por IP nas rotas públicas que custam caro ou revelam dados
 * (login, cadastro, consulta de CPF, frete, checkout de visitante, ferramentas de dev).
 * Janela fixa de 1 minuto, em memória: vale por instância, suficiente com uma só no Railway.
 * Roda depois do Spring Security, então o 429 já sai com os headers de CORS.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
  static final long WINDOW_MILLIS = 60_000;
  private static final int MAX_TRACKED_KEYS = 50_000;

  /** "MÉTODO caminho" → máximo de requisições por minuto por IP. */
  private static final Map<String, Integer> LIMITS = Map.of(
          "POST /api/auth/login", 10,
          "POST /api/auth/refresh", 30,
          "POST /api/auth/signup", 5,
          "POST /api/auth/signup/check-alias", 60,
          "POST /api/auth/signup/check-cpf", 10,
          "POST /api/auth/signup/resend-confirmation", 3,
          "POST /api/shipping/calculate", 30,
          "POST /api/checkout/express-guest", 10,
          "POST /api/dev/test-users", 20
  );

  private final Map<String, Window> windows = new ConcurrentHashMap<>();
  private final JsonMapper jsonMapper;
  private final boolean enabled;

  public RateLimitFilter(JsonMapper jsonMapper, @Value("${app.rate-limit.enabled:true}") boolean enabled) {
    this.jsonMapper = jsonMapper;
    this.enabled = enabled;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !enabled || !LIMITS.containsKey(routeOf(request));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
    String route = routeOf(request);
    long now = System.currentTimeMillis();
    if (windows.size() > MAX_TRACKED_KEYS) {
      windows.values().removeIf(window -> now - window.start >= WINDOW_MILLIS);
    }
    Window window = windows.compute(route + "|" + clientIp(request), (key, current) ->
            current == null || now - current.start >= WINDOW_MILLIS ? new Window(now, 1) : current.increment());

    if (window.count > LIMITS.get(route)) {
      long retryAfter = Math.max(1, (window.start + WINDOW_MILLIS - now + 999) / 1000);
      writeTooManyRequests(request, response, retryAfter);
      return;
    }
    chain.doFilter(request, response);
  }

  private static String routeOf(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.length() > 1 && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return request.getMethod() + " " + path;
  }

  /**
   * No Railway o proxy acrescenta o IP real no fim do X-Forwarded-For. O que vem
   * antes pode ter sido inventado pelo cliente, então vale só o último.
   */
  static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String[] hops = forwarded.split(",");
      String last = hops[hops.length - 1].trim();
      if (!last.isEmpty()) return last;
    }
    return request.getRemoteAddr();
  }

  private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response, long retryAfter)
          throws IOException {
    ApiErrorResponseDTO body = new ApiErrorResponseDTO(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            "Muitas tentativas. Aguarde um pouco e tente de novo.",
            request.getRequestURI(),
            OffsetDateTime.now(),
            null
    );
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getOutputStream(), body);
  }

  private record Window(long start, int count) {
    Window increment() {
      return new Window(start, count + 1);
    }
  }
}
