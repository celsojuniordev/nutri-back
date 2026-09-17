package com.br.nutri.nutritionist.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 255, message = "deve ter no máximo 255 caracteres")
                String name,
        @NotBlank(message = "e-mail é obrigatório")
                @Email(message = "formato de e-mail inválido")
                @Size(max = 255, message = "deve ter no máximo 255 caracteres")
                String email,
        @NotBlank(message = "senha é obrigatória")
                @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                        message = "deve conter ao menos uma letra e um número")
                String password,
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
                @Pattern(regexp = ".*\\S.*", message = "não pode conter apenas espaços em branco")
                String company) {
}
