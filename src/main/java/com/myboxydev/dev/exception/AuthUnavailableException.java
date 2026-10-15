package com.myboxydev.dev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * O Supabase Auth não respondeu (timeout ou falha de rede). Pode ter criado o
 * usuário mesmo assim, por isso o cadastro confere e desfaz antes de responder 503.
 */
public class AuthUnavailableException extends ResponseStatusException {
  public AuthUnavailableException() {
    super(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
  }
}
