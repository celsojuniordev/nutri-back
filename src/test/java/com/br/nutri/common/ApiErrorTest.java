package com.br.nutri.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ApiErrorTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serializesWithoutDetailsWhenThereIsOnlyOneProblem() throws Exception {
        ApiError error = new ApiError(401, "UNAUTHORIZED", "Autenticação necessária.");

        String json = objectMapper.writeValueAsString(error);

        assertThat(json).contains("\"status\":401");
        assertThat(json).contains("\"error\":\"UNAUTHORIZED\"");
        assertThat(json).contains("\"message\":\"Autenticação necessária.\"");
        assertThat(json).contains("\"timestamp\"");
        assertThat(json).doesNotContain("\"details\"");
    }

    @Test
    void serializesWithDetailsWhenMultipleFieldsAreInvalid() throws Exception {
        ApiError error = new ApiError(
                400,
                "VALIDATION_ERROR",
                "Dados inválidos.",
                List.of(new ApiError.FieldError("email", "formato de e-mail inválido")),
                Instant.parse("2026-01-01T00:00:00Z"));

        String json = objectMapper.writeValueAsString(error);

        assertThat(json).contains("\"details\":[{\"field\":\"email\",\"message\":\"formato de e-mail inválido\"}]");
    }
}
