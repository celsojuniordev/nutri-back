package com.br.nutri.auth;

import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GoogleAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Test
    void createsAccountAutomaticallyWhenNoAccountExistsForTheGoogleEmail() throws Exception {
        String email = "google-new-" + UUID.randomUUID() + "@example.com";
        when(googleTokenVerifierService.verify("valid-token"))
                .thenReturn(new GoogleTokenVerifierService.GooglePayload("subject-" + UUID.randomUUID(), email, "Ana Silva"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.accountCreated").value(true));
    }

    @Test
    void linksExistingAccountInsteadOfCreatingADuplicate() throws Exception {
        String email = "google-existing-" + UUID.randomUUID() + "@example.com";
        nutritionistRepository.save(new Nutritionist("Ana Original", "Empresa Original", email, "some-hash", null));
        when(googleTokenVerifierService.verify("valid-token"))
                .thenReturn(new GoogleTokenVerifierService.GooglePayload("subject-" + UUID.randomUUID(), email, "Ana Google"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.accountCreated").value(false));

        Nutritionist reloaded = nutritionistRepository.findByEmailIgnoreCase(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getName()).isEqualTo("Ana Original");
        org.assertj.core.api.Assertions.assertThat(reloaded.getCompany()).isEqualTo("Empresa Original");
        org.assertj.core.api.Assertions.assertThat(reloaded.getGoogleSubject()).isNotNull();
    }

    @Test
    void rejectsInvalidGoogleToken() throws Exception {
        when(googleTokenVerifierService.verify("invalid-token"))
                .thenThrow(new com.br.nutri.common.GoogleTokenInvalidException("Token do Google inválido."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"invalid-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("GOOGLE_TOKEN_INVALID"));
    }

    @Test
    void rejectsGoogleTokenWithUnverifiedEmail() throws Exception {
        when(googleTokenVerifierService.verify("unverified-token"))
                .thenThrow(new com.br.nutri.common.GoogleTokenInvalidException("E-mail da conta Google não verificado."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"unverified-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("GOOGLE_TOKEN_INVALID"));
    }
}
