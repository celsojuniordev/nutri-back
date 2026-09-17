package com.br.nutri.common;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("E-mail já está em uso.");
    }
}
