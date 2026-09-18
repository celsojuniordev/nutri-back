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
class PatientSearchIntegrationTest {

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
    void searchesByPartialName() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        patientRepository.save(activePatient("Carla Dias", LocalDate.of(1992, 3, 3), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("nome", "ANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"));
    }

    @Test
    void searchesBySex() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        patientRepository.save(activePatient("Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("sexo", "MASCULINO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Bruno Souza"));
    }

    @Test
    void searchesByBirthDateRange() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        patientRepository.save(
                activePatient("Bruno Souza", LocalDate.of(2000, 1, 1), Sex.MASCULINO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("dataNascimentoInicio", "1980-01-01")
                        .param("dataNascimentoFim", "1995-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"));
    }

    @Test
    void searchesByPartialEmailOrPhone() throws Exception {
        patientRepository.save(activePatient(
                "Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, "ana@example.com", "11999990000",
                nutritionistId));
        patientRepository.save(activePatient(
                "Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, "bruno@example.com", "11888880000",
                nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("email", "bruno@"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Bruno Souza"));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("telefone", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"));
    }

    @Test
    void combinesMultipleCriteria() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        patientRepository.save(activePatient("Ana Costa", LocalDate.of(1990, 5, 10), Sex.MASCULINO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("nome", "ana")
                        .param("sexo", "FEMININO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"));
    }

    @Test
    void withoutAnyCriteriaReturnsSameResultAsDefaultListing() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Silva"));
    }

    @Test
    void returnsEmptyListWhenNoPatientMatches() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("nome", "Zeca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void doesNotReturnPatientsOfOtherNutritionists() throws Exception {
        patientRepository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, otherNutritionist()));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("nome", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void doesNotReturnInactivePatients() throws Exception {
        patientRepository.save(
                inactivePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));

        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("nome", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void rejectsInvertedBirthDateRange() throws Exception {
        mockMvc.perform(get("/api/pacientes/busca")
                        .header("Authorization", "Bearer " + token)
                        .param("dataNascimentoInicio", "2000-01-02")
                        .param("dataNascimentoFim", "2000-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATE_RANGE"));
    }

    @Test
    void rejectsSearchRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/pacientes/busca")).andExpect(status().isUnauthorized());
    }
}
