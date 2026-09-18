package com.br.nutri.patient;

import static com.br.nutri.patient.PatientTestFactory.activePatient;
import static com.br.nutri.patient.PatientTestFactory.inactivePatient;
import static org.assertj.core.api.Assertions.assertThat;

import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import com.br.nutri.patient.dto.PatientSearchCriteria;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class PatientRepositoryTest {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private NutritionistRepository nutritionistRepository;

    private Long nutritionistId;
    private Long otherNutritionistId;

    @BeforeEach
    void setUp() {
        nutritionistId = newNutritionist().getId();
        otherNutritionistId = newNutritionist().getId();
    }

    private Nutritionist newNutritionist() {
        String email = "nutri-" + UUID.randomUUID() + "@example.com";
        return nutritionistRepository.save(new Nutritionist("Nutricionista", null, email, "hash", null));
    }

    @Test
    void persistsPatientWithAllFieldsIncludingOptionalContact() {
        Patient saved = repository.save(activePatient(
                "Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, "ana@example.com", "11999990000",
                nutritionistId));

        Patient found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFullName()).isEqualTo("Ana Silva");
        assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 10));
        assertThat(found.getSex()).isEqualTo(Sex.FEMININO);
        assertThat(found.getEmail()).isEqualTo("ana@example.com");
        assertThat(found.getPhone()).isEqualTo("11999990000");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getNutritionistId()).isEqualTo(nutritionistId);
    }

    @Test
    void persistsPatientWithoutOptionalContactFields() {
        Patient saved =
                repository.save(activePatient("Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, nutritionistId));

        Patient found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmail()).isNull();
        assertThat(found.getPhone()).isNull();
    }

    @Test
    void findsOnlyActivePatientsOfGivenNutritionist() {
        repository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(inactivePatient("Carla Dias", LocalDate.of(1992, 3, 3), Sex.FEMININO, nutritionistId));
        repository.save(
                activePatient("Outro Paciente", LocalDate.of(1991, 2, 2), Sex.MASCULINO, otherNutritionistId));

        Page<Patient> page = repository.findByNutritionistIdAndActiveTrue(nutritionistId, PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(Patient::getFullName).containsExactly("Ana Silva");
    }

    @Test
    void findsOwnedPatientByIdRegardlessOfActiveStatus() {
        Patient saved =
                repository.save(inactivePatient("Diego Alves", LocalDate.of(1988, 7, 7), Sex.MASCULINO, nutritionistId));

        assertThat(repository.findByIdAndNutritionistId(saved.getId(), nutritionistId)).isPresent();
    }

    @Test
    void doesNotFindPatientOwnedByAnotherNutritionist() {
        Patient saved =
                repository.save(activePatient("Elisa Rocha", LocalDate.of(1995, 4, 4), Sex.FEMININO, otherNutritionistId));

        assertThat(repository.findByIdAndNutritionistId(saved.getId(), nutritionistId)).isEmpty();
    }

    @Test
    void searchMatchesPartialCaseInsensitiveName() {
        repository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(activePatient("Carla Dias", LocalDate.of(1992, 3, 3), Sex.FEMININO, nutritionistId));

        PatientSearchCriteria criteria = new PatientSearchCriteria("ana", null, null, null, null, null);
        Page<Patient> page =
                repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(Patient::getFullName).containsExactly("Ana Silva");
    }

    @Test
    void searchMatchesExactSex() {
        repository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(activePatient("Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, nutritionistId));

        PatientSearchCriteria criteria = new PatientSearchCriteria(null, Sex.MASCULINO, null, null, null, null);
        Page<Patient> page =
                repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(Patient::getFullName).containsExactly("Bruno Souza");
    }

    @Test
    void searchMatchesBirthDateRange() {
        repository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(activePatient("Bruno Souza", LocalDate.of(2000, 1, 1), Sex.MASCULINO, nutritionistId));

        PatientSearchCriteria criteria = new PatientSearchCriteria(
                null, null, LocalDate.of(1980, 1, 1), LocalDate.of(1995, 1, 1), null, null);
        Page<Patient> page =
                repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(Patient::getFullName).containsExactly("Ana Silva");
    }

    @Test
    void searchMatchesPartialEmailAndPhone() {
        repository.save(activePatient(
                "Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, "ana@example.com", "11999990000",
                nutritionistId));
        repository.save(activePatient(
                "Bruno Souza", LocalDate.of(1985, 1, 1), Sex.MASCULINO, "bruno@example.com", "11888880000",
                nutritionistId));

        PatientSearchCriteria byEmail = new PatientSearchCriteria(null, null, null, null, "ana@", null);
        assertThat(repository
                        .findAll(PatientSpecifications.matching(nutritionistId, byEmail), PageRequest.of(0, 25))
                        .getContent())
                .extracting(Patient::getFullName)
                .containsExactly("Ana Silva");

        PatientSearchCriteria byPhone = new PatientSearchCriteria(null, null, null, null, null, "8888");
        assertThat(repository
                        .findAll(PatientSpecifications.matching(nutritionistId, byPhone), PageRequest.of(0, 25))
                        .getContent())
                .extracting(Patient::getFullName)
                .containsExactly("Bruno Souza");
    }

    @Test
    void searchCombinesMultipleCriteriaWithAnd() {
        repository.save(activePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(activePatient("Ana Costa", LocalDate.of(1990, 5, 10), Sex.MASCULINO, nutritionistId));

        PatientSearchCriteria criteria = new PatientSearchCriteria("ana", Sex.FEMININO, null, null, null, null);
        Page<Patient> page =
                repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(Patient::getFullName).containsExactly("Ana Silva");
    }

    @Test
    void searchExcludesInactivePatientsAndOtherNutritionists() {
        repository.save(inactivePatient("Ana Silva", LocalDate.of(1990, 5, 10), Sex.FEMININO, nutritionistId));
        repository.save(activePatient("Ana Souza", LocalDate.of(1990, 5, 10), Sex.FEMININO, otherNutritionistId));

        PatientSearchCriteria criteria = new PatientSearchCriteria("ana", null, null, null, null, null);
        Page<Patient> page =
                repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), PageRequest.of(0, 25));

        assertThat(page.getContent()).isEmpty();
    }
}
