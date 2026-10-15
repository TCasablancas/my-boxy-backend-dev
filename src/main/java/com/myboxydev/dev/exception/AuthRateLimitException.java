package com.myboxydev.dev.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * O Supabase Auth respondeu 429. Repassa ao app o tipo de limite (só no log) e,
 * sempre, o {@code Retry-After} em segundos (o do Supabase ou um padrão).
 */
public class AuthRateLimitException extends ResponseStatusException {
  /** Limite de envio de e-mails do Supabase (e-mail de confirmação do cadastro). */
  public static final String EMAIL_SEND = "over_email_send_rate_limit";
  /** Limite de requisições por IP no Auth; o IP é o do BFF, compartilhado por todos. */
  public static final String REQUEST = "over_request_rate_limit";

  /**
   * Espera sugerida quando o Supabase não manda Retry-After. A cota de e-mail é
   * por hora, então a espera é maior; a de requisições se renova rápido.
   */
  public static final long DEFAULT_EMAIL_SEND_RETRY_AFTER_SECONDS = 600;
  public static final long DEFAULT_RETRY_AFTER_SECONDS = 60;

  private final String errorCode;
  private final Long retryAfterSeconds;

  public AuthRateLimitException(String errorCode, Long retryAfterSeconds) {
    super(HttpStatus.TOO_MANY_REQUESTS, EMAIL_SEND.equals(errorCode)
            ? "Limite de e-mails de confirmação atingido. Tente novamente em alguns minutos."
            : "Muitas tentativas. Tente novamente em instantes.");
    this.errorCode = errorCode;
    this.retryAfterSeconds = retryAfterSeconds != null ? retryAfterSeconds
            : EMAIL_SEND.equals(errorCode) ? DEFAULT_EMAIL_SEND_RETRY_AFTER_SECONDS
            : DEFAULT_RETRY_AFTER_SECONDS;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    return headers;
  }
}
