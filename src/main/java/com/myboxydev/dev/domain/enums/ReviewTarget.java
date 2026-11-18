package com.myboxydev.dev.domain.enums;

/**
 * De quem são as avaliações listadas (ADR-0003). PRODUCT filtra por product_id;
 * STORE filtra por store_id e junta avaliações dos produtos da loja com as da loja.
 */
public enum ReviewTarget {
  PRODUCT, STORE
}
