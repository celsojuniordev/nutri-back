package com.br.nutri.nutritionist.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegisterRequest valid() {
        return new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", null);
    }

    @Test
    void acceptsRequestWithoutCompany() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(valid());

        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsRequestWithCompany() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", "Clínica Vida");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        RegisterRequest request = new RegisterRequest("   ", "ana@example.com", "Senha123", null);

        assertThat(violations(request, "name")).isNotEmpty();
    }

    @Test
    void rejectsMissingEmail() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "", "Senha123", null);

        assertThat(violations(request, "email")).isNotEmpty();
    }

    @Test
    void rejectsInvalidEmailFormat() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "not-an-email", "Senha123", null);

        assertThat(violations(request, "email")).isNotEmpty();
    }

    @Test
    void rejectsShortPassword() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "abc123", null);

        assertThat(violations(request, "password")).isNotEmpty();
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "12345678", null);

        assertThat(violations(request, "password")).isNotEmpty();
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "abcdefgh", null);

        assertThat(violations(request, "password")).isNotEmpty();
    }

    @Test
    void rejectsPasswordLongerThan72Characters() {
        String longPassword = "A1".repeat(40);
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", longPassword, null);

        assertThat(violations(request, "password")).isNotEmpty();
    }

    @Test
    void rejectsNameLongerThan255Characters() {
        RegisterRequest request =
                new RegisterRequest("A".repeat(256), "ana@example.com", "Senha123", null);

        assertThat(violations(request, "name")).isNotEmpty();
    }

    @Test
    void rejectsCompanyLongerThan255Characters() {
        RegisterRequest request =
                new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", "C".repeat(256));

        assertThat(violations(request, "company")).isNotEmpty();
    }

    @Test
    void rejectsCompanyThatIsOnlyWhitespace() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", "   ");

        assertThat(violations(request, "company")).isNotEmpty();
    }

    private Set<ConstraintViolation<RegisterRequest>> violations(RegisterRequest request, String property) {
        return validator.validateProperty(request, property);
    }
}
