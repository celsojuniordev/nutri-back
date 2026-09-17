package com.br.nutri.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void sameRawPasswordProducesDifferentHashesEachTime() {
        String hash1 = passwordEncoder.encode("Sup3rSecret");
        String hash2 = passwordEncoder.encode("Sup3rSecret");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void matchesRecognizesCorrectPassword() {
        String hash = passwordEncoder.encode("Sup3rSecret");

        assertThat(passwordEncoder.matches("Sup3rSecret", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", hash)).isFalse();
    }
}
