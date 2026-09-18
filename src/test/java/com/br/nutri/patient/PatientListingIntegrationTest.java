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
class PatientListingIntegrationTest {

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
    void returnsOnlyActivePatientsOfAuthenticatedNutritionist() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        patientRepository.save(
                inactivePatient("Carla Dias", LocalDate.of(1992, 3, 3), Sex.FEMININO, nutritionistId));
        patientRepository.save(activePatient(
                "Outro Paciente", LocalDate.of(1991, 2, 2), Sex.MASCULINO, otherNutritionist()));

        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsEmptyListWhenNutritionistHasNoPatients() throws Exception {
        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void respectsPagination() throws Exception {
        for (int i = 0; i < 12; i++) {
            patientRepository.save(
                    activePatient("Paciente " + i, LocalDate.of(1990, 1, 1), Sex.FEMININO, nutritionistId));
        }

        mockMvc.perform(get("/api/pacientes")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void appliesDefaultPageSizeOfTwentyFiveWhenNotInformed() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(25));
    }

    @Test
    void rejectsPageSizeOutsideAllowedSet() throws Exception {
        mockMvc.perform(get("/api/pacientes")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAGE_SIZE"));
    }

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
    }
}
