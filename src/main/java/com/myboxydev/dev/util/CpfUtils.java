package com.myboxydev.dev.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CpfUtils {
  public static String cleanCpf(String cpf) {
    if (cpf == null) return null;
    return cpf.replaceAll("[^0-9]", "");
  }

  public static boolean isValidCpf(String cpf) {
    String cleaned = cleanCpf(cpf);
    if (cleaned == null || cleaned.length() != 11 || cleaned.matches("(\\d)\\1{10}")) {
      return false;
    }

    try {
      int sum = 0;
      for (int i = 0; i < 9; i++) {
        sum += (cleaned.charAt(i) - '0') * (10 - i);
      }
      int firstDigit = 11 - (sum % 11);
      if (firstDigit >= 10) firstDigit = 0;

      if (firstDigit != (cleaned.charAt(9) - '0')) return false;

      sum = 0;
      for (int i = 0; i < 10; i++) {
        sum += (cleaned.charAt(i) - '0') * (11 - i);
      }
      int secondDigit = 11 - (sum % 11);
      if (secondDigit >= 10) secondDigit = 0;

      return secondDigit == (cleaned.charAt(10) - '0');
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Hash do CPF para busca e unicidade: HMAC-SHA256 com segredo próprio (pepper).
   * SHA-256 puro não serve: só existem ~1 bilhão de CPFs, e quem tivesse a tabela
   * descobriria todos testando um por um. Sem o pepper, isso não é possível.
   * Igual ao SQL: encode(extensions.hmac(cpf, pepper, 'sha256'), 'hex').
   */
  public static String hashCpf(String rawCpf, String pepper) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalStateException("CPF_HASH_PEPPER deve ser configurado");
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(cleanCpf(rawCpf).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Erro ao calcular o hash do CPF", e);
    }
  }

  /** CPF válido aleatório, só para dados de teste. */
  public static String generateValidCpf() {
    SecureRandom random = new SecureRandom();
    String cpf;
    do {
      StringBuilder base = new StringBuilder();
      for (int i = 0; i < 9; i++) base.append(random.nextInt(10));
      cpf = base + checkDigits(base.toString());
    } while (!isValidCpf(cpf));
    return cpf;
  }

  private static String checkDigits(String nineDigits) {
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (nineDigits.charAt(i) - '0') * (10 - i);
    int first = 11 - (sum % 11);
    if (first >= 10) first = 0;
    sum = 0;
    String ten = nineDigits + first;
    for (int i = 0; i < 10; i++) sum += (ten.charAt(i) - '0') * (11 - i);
    int second = 11 - (sum % 11);
    if (second >= 10) second = 0;
    return "" + first + second;
  }

  public static String maskCpf(String rawCpf) {
    String cleaned = cleanCpf(rawCpf);
    if (cleaned == null || cleaned.length() != 11) return "***.***.***-**";
    return "***." + cleaned.substring(3, 6) + "." + cleaned.substring(6, 9) + "-**";
  }
}
