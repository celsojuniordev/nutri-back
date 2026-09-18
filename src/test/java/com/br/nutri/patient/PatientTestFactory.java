package com.br.nutri.patient;

import java.time.LocalDate;

final class PatientTestFactory {

    private PatientTestFactory() {
    }

    static Patient activePatient(String fullName, LocalDate birthDate, Sex sex, Long nutritionistId) {
        return new Patient(fullName, birthDate, sex, null, null, nutritionistId);
    }

    static Patient activePatient(
            String fullName, LocalDate birthDate, Sex sex, String email, String phone, Long nutritionistId) {
        return new Patient(fullName, birthDate, sex, email, phone, nutritionistId);
    }

    static Patient inactivePatient(String fullName, LocalDate birthDate, Sex sex, Long nutritionistId) {
        Patient patient = activePatient(fullName, birthDate, sex, nutritionistId);
        patient.setActive(false);
        return patient;
    }
}
