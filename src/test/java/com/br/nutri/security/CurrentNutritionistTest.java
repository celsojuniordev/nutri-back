package com.br.nutri.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentNutritionistTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsIdFromAuthenticationPopulatedByTheFilter() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(42L, null, List.of()));

        assertThat(CurrentNutritionist.id()).isEqualTo(42L);
    }

    @Test
    void throwsWhenThereIsNoAuthentication() {
        assertThatThrownBy(CurrentNutritionist::id).isInstanceOf(IllegalStateException.class);
    }
}
