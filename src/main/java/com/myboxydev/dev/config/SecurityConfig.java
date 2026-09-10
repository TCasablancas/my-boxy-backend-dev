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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // 1. ROTAS PÚBLICAS (Acesso livre para visitantes / app deslogado)
                    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/subcategories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/check-alias").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()

                    // Endpoints de infraestrutura e documentação
                    .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                    // 2. ROTAS PROTEGIDAS (Exigem usuário autenticado)
                    .requestMatchers("/api/me/**").authenticated()
                    .requestMatchers("/api/addresses/**").authenticated()
                    .requestMatchers("/api/seller/**").authenticated()
                    .requestMatchers("/api/checkout/**").authenticated()

                    // Todas as outras rotas exigem autenticação
                    .anyRequest().authenticated()
            );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("*")); // Permite consumo pelo Flutter
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}