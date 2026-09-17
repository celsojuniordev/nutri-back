package com.br.nutri.nutritionist;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NutritionistControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueEmail() {
        return "nutri-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registersSuccessfullyWithoutCompany() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Ana Silva","email":"%s","password":"Senha123"}
                                """
                                        .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.company").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registersSuccessfullyWithCompany() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Ana Silva","email":"%s","password":"Senha123","company":"Clínica Vida"}
                                """
                                        .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Clínica Vida"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        String body =
                """
                {"name":"Ana Silva","email":"%s","password":"Senha123"}
                """
                        .formatted(email);

        mockMvc.perform(post("/api/nutricionistas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/nutricionistas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Ana Silva","email":"not-an-email","password":"Senha123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Ana Silva","email":"%s","password":"weak"}
                                """
                                        .formatted(uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNameThatIsOnlyWhitespace() throws Exception {
        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"   ","email":"%s","password":"Senha123"}
                                """
                                        .formatted(uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFieldsAboveMaximumLength() throws Exception {
        String longName = "A".repeat(256);

        mockMvc.perform(post("/api/nutricionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"%s","email":"%s","password":"Senha123"}
                                """
                                        .formatted(longName, uniqueEmail())))
                .andExpect(status().isBadRequest());
    }
}
