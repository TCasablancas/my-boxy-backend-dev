package com.myboxydev.dev.config;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//  private final List<String> allowedOrigins;
//
//  public SecurityConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
//    this.allowedOrigins = allowedOrigins;
//  }
//
//  @Bean
//  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    return http
//            .csrf(AbstractHttpConfigurer::disable)
//            .cors(Customizer.withDefaults())
//            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//            .authorizeHttpRequests(authorize -> authorize
//                    .requestMatchers(HttpMethod.GET,
//                            "/api/categories/**",
//                            "/api/products/**",
//                            "/api/stores/**",
//                            "/api/user_profiles/users/check-alias"
//                    ).permitAll()
//                    .requestMatchers("/api/user_profiles/me/**", "/api/v1/me/**").authenticated()
//                    .anyRequest().denyAll()
//            )
//            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
//            .formLogin(AbstractHttpConfigurer::disable)
//            .httpBasic(AbstractHttpConfigurer::disable)
//            .build();
//  }
//
//  @Bean
//  CorsConfigurationSource corsConfigurationSource() {
//    CorsConfiguration configuration = new CorsConfiguration();
//    configuration.setAllowedOrigins(allowedOrigins);
//    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
//    configuration.setExposedHeaders(List.of("Location"));
//    configuration.setAllowCredentials(false);
//    configuration.setMaxAge(3600L);
//
//    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", configuration);
//    return source;
//  }
//}

import com.myboxydev.dev.dto.response.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final List<String> allowedOrigins;

  public SecurityConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(15));
    return new RestTemplate(requestFactory);
  }

  /** GET liberados para visitante. Fonte única: autorização e resolução do token usam esta lista. */
  private static final String[] PUBLIC_GET_PATHS = {
          "/api/products/**",
          "/api/stores/**",
          "/api/categories/**",
          "/api/menu/**",
          "/api/home/banners",
          "/api/user_profiles/users/check-alias",
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // 1. ROTAS PÚBLICAS (Acesso livre para visitantes / app deslogado)
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/shipping/calculate").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/checkout/express-guest").permitAll()

                    // Endpoints de infraestrutura e documentação
                    .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                    // 2. ROTAS PROTEGIDAS (Exigem usuário autenticado)
                    .requestMatchers("/api/v1/me/**").authenticated()
                    .requestMatchers("/api/user_profiles/**").authenticated()
                    .requestMatchers("/api/checkout/**").authenticated()

                    // Todas as outras rotas exigem autenticação
                    .anyRequest().authenticated()
            )
            // 401/403 no mesmo corpo dos demais erros (ApiErrorResponseDTO), para o app tratar igual
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, ex) ->
                            writeError(jsonMapper, request, response, HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada."))
                    .accessDeniedHandler((request, response, ex) ->
                            writeError(jsonMapper, request, response, HttpStatus.FORBIDDEN, "Acesso negado."))
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .bearerTokenResolver(publicAwareBearerTokenResolver())
                    .authenticationEntryPoint((request, response, ex) ->
                            writeError(jsonMapper, request, response, HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada."))
                    .jwt(Customizer.withDefaults()));

    return http.build();
  }

  /**
   * Em GET público o token é ignorado: um token vencido no app do visitante não pode
   * derrubar o catálogo com 401. Nas demais rotas vale o resolver padrão.
   */
  private static BearerTokenResolver publicAwareBearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    List<RequestMatcher> publicMatchers = Arrays.stream(PUBLIC_GET_PATHS)
            .map(path -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, path))
            .toList();
    return request -> publicMatchers.stream().anyMatch(m -> m.matches(request)) ? null : delegate.resolve(request);
  }

  private static void writeError(JsonMapper jsonMapper, HttpServletRequest request, HttpServletResponse response,
                                 HttpStatus status, String message) throws IOException {
    ApiErrorResponseDTO body = new ApiErrorResponseDTO(
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            OffsetDateTime.now(),
            null
    );
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getOutputStream(), body);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    // O app web precisa ler o tempo de espera de um 429
    config.setExposedHeaders(List.of("Retry-After", "ETag"));
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}