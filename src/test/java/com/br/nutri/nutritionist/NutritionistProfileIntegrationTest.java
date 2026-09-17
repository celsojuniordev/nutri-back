package com.br.nutri.nutritionist;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.nutri.auth.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NutritionistProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void returnsOwnProfileWithoutPasswordWhenAuthenticated() throws Exception {
        String email = "me-" + UUID.randomUUID() + "@example.com";
        Nutritionist nutritionist =
                nutritionistRepository.save(new Nutritionist("Ana Silva", "Clínica X", email, "hash", null));
        String token = jwtService.generateToken(nutritionist.getId());

        mockMvc.perform(get("/api/nutricionistas/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.company").value("Clínica X"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/nutricionistas/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWithMalformedToken() throws Exception {
        mockMvc.perform(get("/api/nutricionistas/me")
                        .header("Authorization", "Bearer definitely-not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWithTamperedToken() throws Exception {
        String token = jwtService.generateToken(1L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get("/api/nutricionistas/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
