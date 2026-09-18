package com.br.nutri.patient.dto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.nutri.common.InvalidDateRangeException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PatientSearchCriteriaTest {

    @Test
    void rejectsStartDateAfterEndDate() {
        assertThatThrownBy(() -> new PatientSearchCriteria(
                        null, null, LocalDate.of(2000, 1, 2), LocalDate.of(2000, 1, 1), null, null))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void acceptsStartDateEqualToEndDate() {
        LocalDate date = LocalDate.of(2000, 1, 1);
        assertThatCode(() -> new PatientSearchCriteria(null, null, date, date, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsWhenOnlyOneOfTheDatesIsInformed() {
        assertThatCode(() ->
                        new PatientSearchCriteria(null, null, LocalDate.of(2000, 1, 1), null, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                        new PatientSearchCriteria(null, null, null, LocalDate.of(2000, 1, 1), null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsAllCriteriaAbsent() {
        assertThatCode(() -> new PatientSearchCriteria(null, null, null, null, null, null))
                .doesNotThrowAnyException();
    }
}
