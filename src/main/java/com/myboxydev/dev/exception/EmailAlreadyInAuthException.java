package com.myboxydev.dev.exception;

/**
 * O Supabase Auth recusou o cadastro porque o e-mail já existe em auth.users.
 * Responde 409 como as demais regras de negócio; o motivo vai só para o log.
 */
public class EmailAlreadyInAuthException extends BusinessRuleException {
  private final String reason;

  public EmailAlreadyInAuthException(String reason) {
    super("Já existe uma conta associada a este e-mail.");
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }
}
