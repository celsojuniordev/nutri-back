package com.br.nutri.nutritionist;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionistRepository extends JpaRepository<Nutritionist, Long> {

    Optional<Nutritionist> findByEmailIgnoreCase(String email);

    Optional<Nutritionist> findByGoogleSubject(String googleSubject);
}
