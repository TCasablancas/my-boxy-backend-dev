package com.myboxydev.dev.exception;

import com.myboxydev.dev.dto.response.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    ApiErrorResponseDTO error = new ApiErrorResponseDTO(
      HttpStatus.NOT_FOUND.value(),
      "Not Found",
      ex.getMessage(),
      request.getRequestURI(),
      OffsetDateTime.now(),
      null
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler({AliasAlreadyExistsException.class, CpfAlreadyExistsException.class, BusinessRuleException.class})
  public ResponseEntity<ApiErrorResponseDTO> handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
    ApiErrorResponseDTO error = new ApiErrorResponseDTO(
      HttpStatus.CONFLICT.value(),
      "Conflict / Business Rule Violation",
      ex.getMessage(),
      request.getRequestURI(),
      OffsetDateTime.now(),
      null
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiErrorResponseDTO.FieldErrorDTO> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new ApiErrorResponseDTO.FieldErrorDTO(err.getField(), err.getDefaultMessage()))
            .toList();

    ApiErrorResponseDTO error = new ApiErrorResponseDTO(
      HttpStatus.BAD_REQUEST.value(),
      "Validation Error",
      "Um ou mais campos do formulário contêm erros",
      request.getRequestURI(),
      OffsetDateTime.now(),
      fieldErrors
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
    ApiErrorResponseDTO error = new ApiErrorResponseDTO(
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      "Internal Server Error",
      "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
      request.getRequestURI(),
      OffsetDateTime.now(),
      null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
