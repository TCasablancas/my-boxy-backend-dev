package com.myboxydev.dev.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponseDTO(
    int status,
    String error,
    String message,
    String path,
    OffsetDateTime timestamp,
    List<FieldErrorDTO> fieldErrors
) {
  public record FieldErrorDTO(String field, String message) {}
}
