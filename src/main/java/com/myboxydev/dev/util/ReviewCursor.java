package com.myboxydev.dev.util;

import com.myboxydev.dev.exception.FieldValidationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

/**
 * Cursor opaco da lista de avaliações: posição (created_at, id) do último item
 * entregue, em Base64 URL-safe. O app só guarda e devolve, nunca interpreta.
 */
public record ReviewCursor(OffsetDateTime createdAt, UUID id) {
  /** Primeira página: tudo é "anterior" a este ponto. */
  public static final ReviewCursor START = new ReviewCursor(
          OffsetDateTime.of(9999, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC),
          UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

  private static final int MAX_LENGTH = 200;

  public String encode() {
    String raw = createdAt.toInstant() + "|" + id;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  /** null ou vazio = primeira página; qualquer outra coisa ilegível = 400 no campo cursor. */
  public static ReviewCursor decode(String value) {
    if (value == null || value.isBlank()) return START;
    if (value.length() > MAX_LENGTH) throw invalid();
    try {
      String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
      int sep = raw.indexOf('|');
      if (sep < 0) throw invalid();
      Instant at = Instant.parse(raw.substring(0, sep));
      UUID id = UUID.fromString(raw.substring(sep + 1));
      return new ReviewCursor(at.atOffset(ZoneOffset.UTC), id);
    } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
      throw invalid();
    }
  }

  private static FieldValidationException invalid() {
    return new FieldValidationException("cursor", "Cursor inválido. Recomece a lista sem cursor.");
  }
}
