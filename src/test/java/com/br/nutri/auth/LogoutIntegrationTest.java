package com.br.nutri.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LogoutIntegrationTest.ProbeController.class)
class LogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    private String tokenFrom(String responseBody) {
        return JsonPath.read(responseBody, "$.token");
    }

    @Test
    void logoutSucceedsWithValidToken() throws Exception {
        String token = jwtService.generateToken(1L);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void logoutFailsWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenIssuedByTraditionalLoginIsRejectedOnProtectedEndpointAfterLogout() throws Exception {
        String email = "logout-traditional-" + UUID.randomUUID() + "@example.com";
        nutritionistRepository.save(
                new Nutritionist("Ana Silva", null, email, passwordEncoder.encode("Senha123"), null));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Senha123"}
                                """.formatted(email)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = tokenFrom(loginResponse);

        assertTokenRejectedAfterLogout(token);
    }

    @Test
    void tokenIssuedByGoogleLoginIsRejectedOnProtectedEndpointAfterLogout() throws Exception {
        String email = "logout-google-" + UUID.randomUUID() + "@example.com";
        when(googleTokenVerifierService.verify("valid-token"))
                .thenReturn(new GoogleTokenVerifierService.GooglePayload(
                        "subject-" + UUID.randomUUID(), email, "Ana Silva"));

        String googleLoginResponse = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-token"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = tokenFrom(googleLoginResponse);

        assertTokenRejectedAfterLogout(token);
    }

    private void assertTokenRejectedAfterLogout(String token) throws Exception {
        mockMvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/test/probe")
        String probe() {
            return "ok";
        }
    }
}
