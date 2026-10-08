package com.myboxydev.dev;

import com.myboxydev.dev.config.SupabaseProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SupabasePropertiesTests {
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(Config.class)
          .withPropertyValues("app.supabase.auth-url=https://example.test/auth/v1");

  @Test
  void failsStartupWithoutKeys() {
    runner.withPropertyValues("app.supabase.anon-key=", "app.supabase.service-role-key=")
            .run(context -> assertThat(context).hasFailed()
                    .getFailure().rootCause().hasMessageContaining("SUPABASE_ANON_KEY"));
  }

  @Test
  void startsWithKeys() {
    runner.withPropertyValues("app.supabase.anon-key=anon", "app.supabase.service-role-key=service")
            .run(context -> assertThat(context).hasNotFailed());
  }

  @Configuration
  @EnableConfigurationProperties(SupabaseProperties.class)
  static class Config {}
}
