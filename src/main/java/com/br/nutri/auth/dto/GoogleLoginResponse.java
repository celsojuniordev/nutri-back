package com.br.nutri.auth.dto;

public record GoogleLoginResponse(String token, boolean accountCreated) {
}
