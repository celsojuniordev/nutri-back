package com.br.nutri.patient;

import static com.br.nutri.patient.PatientTestFactory.activePatient;
import static com.br.nutri.patient.PatientTestFactory.inactivePatient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.nutri.auth.JwtService;
import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PatientDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtService jwtService;

    private Long nutritionistId;
    private String token;

    @BeforeEach
    void setUp() {
        String email = "nutri-" + UUID.randomUUID() + "@example.com";
        Nutritionist nutritionist =
                nutritionistRepository.save(new Nutritionist("Nutricionista", null, email, "hash", null));
        nutritionistId = nutritionist.getId();
        token = jwtService.generateToken(nutritionistId);
    }

    private Long otherNutritionist() {
        String email = "outro-" + UUID.randomUUID() + "@example.com";
        return nutritionistRepository
                .save(new Nutritionist("Outra Pessoa", null, email, "hash", null))
                .getId();
    }

    @Test
    void returnsPatientDetailWhenOwnedByAuthenticatedNutritionist() throws Exception {
        Patient patient = patientRepository.save(
                activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/{id}", patient.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Silva"));
    }

    @Test
    void rejectsDetailOfPatientOwnedByAnotherNutritionist() throws Exception {
        Patient patient = patientRepository.save(
                activePatient("Elisa Rocha", LocalDate.of(1995, 4, 4), Sex.FEMININO, otherNutritionist()));

        mockMvc.perform(get("/api/pacientes/{id}", patient.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForNonExistentPatient() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", 999999L).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsInactivePatientDetail() throws Exception {
        Patient patient = patientRepository.save(
                inactivePatient("Diego Alves", LocalDate.of(1988, 7, 7), Sex.MASCULINO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/{id}", patient.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void rejectsDetailRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", 1L)).andExpect(status().isUnauthorized());
    }
}
