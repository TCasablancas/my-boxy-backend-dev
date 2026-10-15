package com.myboxydev.dev.exception;

/**
 * O e-mail já pertence a uma conta (no Supabase Auth ou em user_profiles).
 * Responde 409 com o campo {@code email}; o motivo vai só para o log.
 */
public class EmailAlreadyInAuthException extends FieldConflictException {
  public static final String MESSAGE = "Já existe uma conta associada a este e-mail.";

  private final String reason;

  public EmailAlreadyInAuthException(String reason) {
    super("email", MESSAGE);
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }
}
