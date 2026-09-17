package com.br.nutri.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class LoginRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRequestWithEmailAndPassword() {
        assertThat(validator.validate(new LoginRequest("ana@example.com", "Senha123"))).isEmpty();
    }

    @Test
    void rejectsBlankEmail() {
        assertThat(validator.validateProperty(new LoginRequest("   ", "Senha123"), "email"))
                .isNotEmpty();
    }

    @Test
    void rejectsBlankPassword() {
        assertThat(validator.validateProperty(new LoginRequest("ana@example.com", "   "), "password"))
                .isNotEmpty();
    }
}
