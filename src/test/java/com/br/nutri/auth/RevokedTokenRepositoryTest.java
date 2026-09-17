package com.br.nutri.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class RevokedTokenRepositoryTest {

    @Autowired
    private RevokedTokenRepository repository;

    @Test
    void findsInsertedJti() {
        repository.save(new RevokedToken("jti-123", Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThat(repository.existsByJti("jti-123")).isTrue();
        assertThat(repository.existsByJti("unknown-jti")).isFalse();
    }

    @Test
    void rejectsDuplicateJti() {
        repository.save(new RevokedToken("duplicate-jti", Instant.now().plus(1, ChronoUnit.HOURS)));

        assertThatThrownBy(() ->
                        repository.saveAndFlush(new RevokedToken("duplicate-jti", Instant.now().plus(2, ChronoUnit.HOURS))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
