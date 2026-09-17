package com.br.nutri.nutritionist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class NutritionistRepositoryTest {

    @Autowired
    private NutritionistRepository repository;

    @Test
    void findsByEmailIgnoringCase() {
        repository.save(new Nutritionist("Ana Silva", null, "ana@example.com", "hash", null));

        assertThat(repository.findByEmailIgnoreCase("ANA@EXAMPLE.COM")).isPresent();
    }

    @Test
    void findsByGoogleSubject() {
        repository.save(new Nutritionist("Ana Silva", null, "ana@example.com", null, "google-sub-123"));

        assertThat(repository.findByGoogleSubject("google-sub-123")).isPresent();
    }

    @Test
    void rejectsDuplicateEmail() {
        repository.save(new Nutritionist("Ana Silva", null, "dup@example.com", "hash", null));

        assertThatThrownBy(() ->
                        repository.saveAndFlush(new Nutritionist("Outra Pessoa", null, "dup@example.com", "hash2", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateGoogleSubject() {
        repository.save(new Nutritionist("Ana Silva", null, "ana@example.com", null, "same-subject"));

        assertThatThrownBy(() -> repository.saveAndFlush(
                        new Nutritionist("Outra Pessoa", null, "outra@example.com", null, "same-subject")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constructorRejectsNutritionistWithoutPasswordOrGoogleSubject() {
        assertThatThrownBy(() -> new Nutritionist("Ana Silva", null, "ana@example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
