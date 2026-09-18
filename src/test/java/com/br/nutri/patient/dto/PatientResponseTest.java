package com.br.nutri.patient.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.nutri.patient.Sex;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class PatientResponseTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serializesExactlyTheExpectedFields() throws Exception {
        PatientResponse response = new PatientResponse(
                1L, "Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, "ana@example.com", "11999990000", true);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"fullName\":\"Ana Silva\"");
        assertThat(json).contains("\"birthDate\":\"1990-05-10\"");
        assertThat(json).contains("\"sex\":\"FEMININO\"");
        assertThat(json).contains("\"email\":\"ana@example.com\"");
        assertThat(json).contains("\"phone\":\"11999990000\"");
        assertThat(json).contains("\"active\":true");
    }

    @Test
    void omitsOptionalContactFieldsWhenAbsent() throws Exception {
        PatientResponse response =
                new PatientResponse(1L, "Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, null, null, true);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("\"email\"");
        assertThat(json).doesNotContain("\"phone\"");
    }
}
