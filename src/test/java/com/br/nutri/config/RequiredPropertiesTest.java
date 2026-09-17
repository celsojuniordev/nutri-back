package com.br.nutri.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RequiredPropertiesTest {

    private static final String VALID_SECRET = "some-secret-at-least-32-characters-long";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnlyConfig.class);

    @Test
    void applicationFailsToStartWithoutJwtSecret() {
        contextRunner
                .withPropertyValues("app.jwt.expiration-seconds=3600")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationFailsToStartWithoutJwtExpiration() {
        contextRunner
                .withPropertyValues("app.jwt.secret=" + VALID_SECRET)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationFailsWithJwtSecretShorterThan32Characters() {
        contextRunner
                .withPropertyValues("app.jwt.secret=too-short", "app.jwt.expiration-seconds=3600")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationFailsToStartWithoutGoogleClientId() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationFailsToStartWithoutAllowedOrigins() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=" + VALID_SECRET,
                        "app.jwt.expiration-seconds=3600",
                        "app.google.client-id=some-client-id")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationStartsWhenAllRequiredPropertiesArePresent() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=" + VALID_SECRET,
                        "app.jwt.expiration-seconds=3600",
                        "app.google.client-id=some-client-id",
                        "app.security.allowed-origins=http://localhost:5173")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(VALID_SECRET);
                    assertThat(context.getBean(GoogleProperties.class).clientId()).isEqualTo("some-client-id");
                    assertThat(context.getBean(SecurityCorsProperties.class).allowedOrigins())
                            .containsExactly("http://localhost:5173");
                });
    }

    @EnableAutoConfiguration
    @EnableConfigurationProperties({JwtProperties.class, GoogleProperties.class, SecurityCorsProperties.class})
    static class PropertiesOnlyConfig {
    }
}
