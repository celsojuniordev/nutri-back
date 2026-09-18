package com.br.nutri.patient;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Page<Patient> findByNutritionistIdAndActiveTrue(Long nutritionistId, Pageable pageable);

    Optional<Patient> findByIdAndNutritionistId(Long id, Long nutritionistId);
}
