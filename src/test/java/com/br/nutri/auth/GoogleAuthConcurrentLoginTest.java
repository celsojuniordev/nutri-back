package com.br.nutri.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.br.nutri.nutritionist.NutritionistRepository;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class GoogleAuthConcurrentLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Test
    void concurrentGoogleLoginsForTheSameNewEmailResultInASingleAccount() throws Exception {
        String email = "google-race-" + UUID.randomUUID() + "@example.com";
        when(googleTokenVerifierService.verify("valid-token"))
                .thenReturn(new GoogleTokenVerifierService.GooglePayload("subject-" + UUID.randomUUID(), email, "Ana Silva"));

        int attempts = 5;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            Callable<Integer> task = () -> mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"idToken":"valid-token"}
                                    """))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            var futures = IntStream.range(0, attempts)
                    .<Callable<Integer>>mapToObj(i -> task)
                    .map(executor::submit)
                    .toList();

            for (Future<Integer> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(nutritionistRepository.findByEmailIgnoreCase(email)).isPresent();
    }
}
