package com.br.nutri.patient.dto;

import com.br.nutri.common.InvalidDateRangeException;
import com.br.nutri.patient.Sex;
import java.time.LocalDate;

public record PatientSearchCriteria(
        String nome,
        Sex sexo,
        LocalDate dataNascimentoInicio,
        LocalDate dataNascimentoFim,
        String email,
        String telefone) {

    public PatientSearchCriteria {
        if (dataNascimentoInicio != null
                && dataNascimentoFim != null
                && dataNascimentoInicio.isAfter(dataNascimentoFim)) {
            throw new InvalidDateRangeException();
        }
    }
}
