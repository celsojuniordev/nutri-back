package com.br.nutri.patient;

import com.br.nutri.common.PageSizePolicy;
import com.br.nutri.common.PatientNotFoundException;
import com.br.nutri.patient.dto.PatientSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public Page<Patient> listActive(Long nutritionistId, Pageable pageable) {
        PageSizePolicy.validate(pageable);
        return repository.findByNutritionistIdAndActiveTrue(nutritionistId, pageable);
    }

    public Patient getOwnedById(Long nutritionistId, Long patientId) {
        return repository
                .findByIdAndNutritionistId(patientId, nutritionistId)
                .orElseThrow(PatientNotFoundException::new);
    }

    public Page<Patient> search(Long nutritionistId, PatientSearchCriteria criteria, Pageable pageable) {
        PageSizePolicy.validate(pageable);
        return repository.findAll(PatientSpecifications.matching(nutritionistId, criteria), pageable);
    }
}
