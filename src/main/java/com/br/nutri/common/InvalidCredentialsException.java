package com.br.nutri.common;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
