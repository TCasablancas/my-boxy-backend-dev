package com.myboxydev.dev.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.time.Clock;

@Configuration
public class HomeWebConfig {

  /** Relógio injetável: a janela de exibição dos banners é testada com hora fixa. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * ETag nas rotas da home: o app manda If-None-Match e recebe 304 sem corpo
   * quando nada mudou. Restrito a estas rotas para não mudar o resto da API.
   */
  @Bean
  public FilterRegistrationBean<ShallowEtagHeaderFilter> homeEtagFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
    registration.addUrlPatterns("/api/home/banners", "/api/v1/me/home-header");
    registration.setName("homeEtagFilter");
    return registration;
  }
}
