package com.myboxydev.dev.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * O Supabase Auth respondeu 429. Repassa ao app o tipo de limite (só no log) e,
 * quando o Supabase informa, o {@code Retry-After} em segundos.
 */
public class AuthRateLimitException extends ResponseStatusException {
  /** Limite de envio de e-mails do Supabase (e-mail de confirmação do cadastro). */
  public static final String EMAIL_SEND = "over_email_send_rate_limit";
  /** Limite de requisições por IP no Auth; o IP é o do BFF, compartilhado por todos. */
  public static final String REQUEST = "over_request_rate_limit";

  private final String errorCode;
  private final Long retryAfterSeconds;

  public AuthRateLimitException(String errorCode, Long retryAfterSeconds) {
    super(HttpStatus.TOO_MANY_REQUESTS, EMAIL_SEND.equals(errorCode)
            ? "Limite de e-mails de confirmação atingido. Tente novamente em alguns minutos."
            : "Muitas tentativas. Tente novamente em instantes.");
    this.errorCode = errorCode;
    this.retryAfterSeconds = retryAfterSeconds;
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
    if (retryAfterSeconds != null) {
      headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    }
    return headers;
  }
}
