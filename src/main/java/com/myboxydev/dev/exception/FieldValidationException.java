package com.myboxydev.dev.exception;

/**
 * Dado inválido detectado no serviço (fora do bean validation), como CPF com
 * dígito errado ou senha recusada pelo Supabase. Responde 400 e indica o campo
 * em {@code fieldErrors}, no mesmo formato da validação do DTO, para o app
 * marcar o campo e voltar à etapa certa.
 */
public class FieldValidationException extends RuntimeException {
  private final String field;

  public FieldValidationException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
