package com.br.nutri.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "e-mail é obrigatório") String email,
        @NotBlank(message = "senha é obrigatória") String password) {
}
