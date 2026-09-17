package com.br.nutri.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginSucceedsWithCorrectCredentials() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        nutritionistRepository.save(
                new Nutritionist("Ana Silva", null, email, passwordEncoder.encode("Senha123"), null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Senha123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginRejectsWrongPasswordWithoutIssuingToken() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        nutritionistRepository.save(
                new Nutritionist("Ana Silva", null, email, passwordEncoder.encode("Senha123"), null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPass1"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"email":"unknown-%s@example.com","password":"Senha123"}
                                """
                                        .formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginRejectsAccountCreatedOnlyViaGoogle() throws Exception {
        String email = "google-only-" + UUID.randomUUID() + "@example.com";
        nutritionistRepository.save(
                new Nutritionist("Ana Silva", null, email, null, "google-subject-" + UUID.randomUUID()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Senha123"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }
}
