package com.br.nutri.nutritionist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.nutri.common.EmailAlreadyInUseException;
import com.br.nutri.nutritionist.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class NutritionistServiceTest {

    @Mock
    private NutritionistRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private NutritionistService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new NutritionistService(repository, passwordEncoder);
    }

    @Test
    void registersNutritionistWithHashedPasswordAndLowercasedEmail() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "Ana@Example.com", "Senha123", "Clínica X");
        when(repository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("hashed-password");
        when(repository.saveAndFlush(any(Nutritionist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nutritionist result = service.register(request);

        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getCompany()).isEqualTo("Clínica X");
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", null);
        when(repository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(new Nutritionist("Outra", null, "ana@example.com", "hash", null)));

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyInUseException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentDuplicateInsertIntoEmailAlreadyInUseException() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@example.com", "Senha123", null);
        when(repository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("hashed-password");
        when(repository.saveAndFlush(any(Nutritionist.class)))
                .thenThrow(new DataIntegrityViolationException("uk_nutritionists_email"));

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyInUseException.class);
    }
}
