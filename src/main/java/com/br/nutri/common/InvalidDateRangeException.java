package com.br.nutri.common;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("A data de nascimento inicial não pode ser posterior à data de nascimento final.");
    }
}
