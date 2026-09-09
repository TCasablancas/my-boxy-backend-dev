package com.myboxydev.dev.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

  public static String calculateSha256Hash(String rawCpf) {
    try {
      String cleaned = cleanCpf(rawCpf);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(cleaned.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Erro ao calcular hash SHA-256 do CPF", e);
    }
  }

  public static String maskCpf(String rawCpf) {
    String cleaned = cleanCpf(rawCpf);
    if (cleaned == null || cleaned.length() != 11) return "***.***.***-**";
    return "***." + cleaned.substring(3, 6) + "." + cleaned.substring(6, 9) + "-**";
  }
}
