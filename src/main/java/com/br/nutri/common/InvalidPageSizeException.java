package com.br.nutri.common;

public class InvalidPageSizeException extends RuntimeException {

    public InvalidPageSizeException() {
        super("Tamanho de página inválido. Valores aceitos: 10, 25 ou 50.");
    }
}
