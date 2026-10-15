package com.myboxydev.dev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login com e-mail e senha corretos, mas o e-mail ainda não foi confirmado.
 * Responde 403 (diferente do 401 de credenciais erradas) para o app oferecer
 * o reenvio do link de confirmação.
 */
public class EmailNotConfirmedException extends ResponseStatusException {
  public static final String MESSAGE = "Confirme seu e-mail para entrar. Abra o link que enviamos ou peça um novo.";

  public EmailNotConfirmedException() {
    super(HttpStatus.FORBIDDEN, MESSAGE);
  }
}
