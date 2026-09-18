package com.br.nutri.common;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException() {
        super("Paciente não encontrado.");
    }
}
