package com.br.nutri.security;

import com.br.nutri.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigIntegrationTest.ProbeController.class)
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/test/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsRequestWithValidToken() throws Exception {
        String token = jwtService.generateToken(1L);

        mockMvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightReturnsAllowedOriginHeader() throws Exception {
        mockMvc.perform(options("/test/probe")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void authenticatedPostIsNotBlockedByCsrfProtection() throws Exception {
        String token = jwtService.generateToken(1L);

        mockMvc.perform(post("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/test/probe")
        String get() {
            return "ok";
        }

        @PostMapping("/test/probe")
        String post() {
            return "ok";
        }
    }
}
