package com.myboxydev.dev;

import com.myboxydev.dev.util.CpfUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfUtilsTests {

  @Test
  void hashIsHmacSha256WithPepperLikePgcrypto() {
    // Mesmo valor de: encode(extensions.hmac('52998224725', 'test-pepper', 'sha256'), 'hex')
    assertThat(CpfUtils.hashCpf("529.982.247-25", "test-pepper"))
            .isEqualTo("6ca286d63d0fdb77be18ea21406945f81d085aad6f135c8d88cfe749eee4e69f");
  }

  @Test
  void hashChangesWithPepper() {
    assertThat(CpfUtils.hashCpf("52998224725", "a")).isNotEqualTo(CpfUtils.hashCpf("52998224725", "b"));
  }

  @Test
  void hashWithoutPepperFails() {
    assertThatThrownBy(() -> CpfUtils.hashCpf("52998224725", " ")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void generatedCpfsAreValid() {
    for (int i = 0; i < 200; i++) {
      assertThat(CpfUtils.isValidCpf(CpfUtils.generateValidCpf())).isTrue();
    }
  }
}
