package com.br.nutri.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.nutri.auth.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String bearerToken() {
        return "Bearer " + jwtService.generateToken(1L);
    }

    @Test
    void mapsEmailAlreadyInUseTo409() throws Exception {
        mockMvc.perform(get("/test/errors/email-in-use").header("Authorization", bearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void mapsDataIntegrityViolationTo409() throws Exception {
        mockMvc.perform(get("/test/errors/data-integrity-violation").header("Authorization", bearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void mapsInvalidCredentialsTo401() throws Exception {
        mockMvc.perform(get("/test/errors/invalid-credentials").header("Authorization", bearerToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void mapsGoogleTokenInvalidTo401() throws Exception {
        mockMvc.perform(get("/test/errors/google-token-invalid").header("Authorization", bearerToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("GOOGLE_TOKEN_INVALID"));
    }

    @Test
    void mapsValidationFailureTo400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .header("Authorization", bearerToken())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("value"));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/test/errors/email-in-use")
        String emailInUse() {
            throw new EmailAlreadyInUseException();
        }

        @GetMapping("/test/errors/data-integrity-violation")
        String dataIntegrityViolation() {
            throw new DataIntegrityViolationException("uk_nutritionists_email violated");
        }

        @GetMapping("/test/errors/invalid-credentials")
        String invalidCredentials() {
            throw new InvalidCredentialsException();
        }

        @GetMapping("/test/errors/google-token-invalid")
        String googleTokenInvalid() {
            throw new GoogleTokenInvalidException("token inválido");
        }

        @PostMapping("/test/errors/validation")
        String validation(@Valid @RequestBody ProbeBody body) {
            return body.value();
        }
    }

    record ProbeBody(@NotBlank String value) {
    }
}
