package com.br.nutri.patient;

import com.br.nutri.patient.dto.PatientSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class PatientSpecifications {

    private PatientSpecifications() {
    }

    static Specification<Patient> matching(Long nutritionistId, PatientSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("nutritionistId"), nutritionistId));
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            if (criteria.nome() != null && !criteria.nome().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        "%" + criteria.nome().toLowerCase(Locale.ROOT) + "%"));
            }
            if (criteria.sexo() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sex"), criteria.sexo()));
            }
            if (criteria.dataNascimentoInicio() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("birthDate"), criteria.dataNascimentoInicio()));
            }
            if (criteria.dataNascimentoFim() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("birthDate"), criteria.dataNascimentoFim()));
            }
            if (criteria.email() != null && !criteria.email().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + criteria.email().toLowerCase(Locale.ROOT) + "%"));
            }
            if (criteria.telefone() != null && !criteria.telefone().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("phone"), "%" + criteria.telefone() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
