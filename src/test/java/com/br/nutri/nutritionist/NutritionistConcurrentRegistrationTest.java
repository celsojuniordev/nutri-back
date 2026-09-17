package com.br.nutri.nutritionist;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NutritionistConcurrentRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionistRepository repository;

    @Test
    void exactlyOneConcurrentRegistrationWithTheSameEmailSucceeds() throws Exception {
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";
        String body =
                """
                {"name":"Ana Silva","email":"%s","password":"Senha123"}
                """
                        .formatted(email);

        int attempts = 5;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            Callable<Integer> task = () -> mockMvc.perform(org.springframework.test.web.servlet.request
                            .MockMvcRequestBuilders.post("/api/nutricionistas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            var futures = IntStream.range(0, attempts)
                    .<Callable<Integer>>mapToObj(i -> task)
                    .map(executor::submit)
                    .toList();

            long created = 0;
            long conflicted = 0;
            for (Future<Integer> future : futures) {
                int status = future.get(10, TimeUnit.SECONDS);
                if (status == 201) {
                    created++;
                } else if (status == 409) {
                    conflicted++;
                }
            }

            assertThat(created).isEqualTo(1);
            assertThat(conflicted).isEqualTo(attempts - 1);
            assertThat(repository.findByEmailIgnoreCase(email)).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }
}
