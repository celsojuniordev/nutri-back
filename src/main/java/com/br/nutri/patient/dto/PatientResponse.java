package com.br.nutri.patient.dto;

import com.br.nutri.patient.Patient;
import com.br.nutri.patient.Sex;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientResponse(
        Long id, String fullName, LocalDate birthDate, Sex sex, String email, String phone, boolean active) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getBirthDate(),
                patient.getSex(),
                patient.getEmail(),
                patient.getPhone(),
                patient.isActive());
    }
}
