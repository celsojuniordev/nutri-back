package com.br.nutri.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.nutri.common.InvalidPageSizeException;
import com.br.nutri.common.PatientNotFoundException;
import com.br.nutri.patient.dto.PatientSearchCriteria;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository repository;

    private PatientService service;

    @BeforeEach
    void setUp() {
        service = new PatientService(repository);
    }

    @Test
    void listActiveDelegatesToRepositoryWithGivenNutritionistAndPageable() {
        Patient patient = new Patient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, null, null, 1L);
        Pageable pageable = PageRequest.of(0, 25);
        Page<Patient> page = new PageImpl<>(List.of(patient), pageable, 1);
        when(repository.findByNutritionistIdAndActiveTrue(1L, pageable)).thenReturn(page);

        Page<Patient> result = service.listActive(1L, pageable);

        assertThat(result.getContent()).containsExactly(patient);
        verify(repository).findByNutritionistIdAndActiveTrue(1L, pageable);
    }

    @Test
    void getOwnedByIdReturnsPatientWhenFound() {
        Patient patient = new Patient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, null, null, 1L);
        when(repository.findByIdAndNutritionistId(10L, 1L)).thenReturn(Optional.of(patient));

        Patient result = service.getOwnedById(1L, 10L);

        assertThat(result).isEqualTo(patient);
    }

    @Test
    void getOwnedByIdThrowsWhenPatientDoesNotExist() {
        when(repository.findByIdAndNutritionistId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedById(1L, 10L)).isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void getOwnedByIdThrowsWhenPatientBelongsToAnotherNutritionist() {
        when(repository.findByIdAndNutritionistId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedById(1L, 10L)).isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void searchDelegatesToRepositoryWithSpecificationAndPageable() {
        Patient patient = new Patient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, null, null, 1L);
        Pageable pageable = PageRequest.of(0, 25);
        Page<Patient> page = new PageImpl<>(List.of(patient), pageable, 1);
        PatientSearchCriteria criteria = new PatientSearchCriteria(null, null, null, null, null, null);
        when(repository.findAll(ArgumentMatchers.<Specification<Patient>>any(), eq(pageable))).thenReturn(page);

        Page<Patient> result = service.search(1L, criteria, pageable);

        assertThat(result.getContent()).containsExactly(patient);
        verify(repository).findAll(ArgumentMatchers.<Specification<Patient>>any(), eq(pageable));
    }

    @Test
    void searchRejectsPageSizeOutsideAllowedSet() {
        PatientSearchCriteria criteria = new PatientSearchCriteria(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.search(1L, criteria, PageRequest.of(0, 15)))
                .isInstanceOf(InvalidPageSizeException.class);
    }
}
