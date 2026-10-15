package com.myboxydev.dev.exception;

/**
 * Conflito real com um dado único do usuário (e-mail, alias ou CPF).
 * Responde 409 e indica o campo em {@code fieldErrors}, para o app marcar o
 * campo certo do formulário.
 */
public class FieldConflictException extends BusinessRuleException {
  private final String field;

  public FieldConflictException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
