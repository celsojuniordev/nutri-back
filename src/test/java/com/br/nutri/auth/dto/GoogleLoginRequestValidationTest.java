package com.br.nutri.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class GoogleLoginRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRequestWithIdToken() {
        assertThat(validator.validate(new GoogleLoginRequest("some-id-token"))).isEmpty();
    }

    @Test
    void rejectsMissingIdToken() {
        assertThat(validator.validate(new GoogleLoginRequest(""))).isNotEmpty();
    }

    @Test
    void rejectsBlankIdToken() {
        assertThat(validator.validate(new GoogleLoginRequest("   "))).isNotEmpty();
    }
}
