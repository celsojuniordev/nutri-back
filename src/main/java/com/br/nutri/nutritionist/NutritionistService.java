package com.br.nutri.nutritionist;

import com.br.nutri.common.EmailAlreadyInUseException;
import com.br.nutri.nutritionist.dto.RegisterRequest;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutritionistService {

    private final NutritionistRepository repository;
    private final PasswordEncoder passwordEncoder;

    public NutritionistService(NutritionistRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Nutritionist register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new EmailAlreadyInUseException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Nutritionist nutritionist =
                new Nutritionist(request.name(), request.company(), normalizedEmail, passwordHash, null);
        try {
            return repository.saveAndFlush(nutritionist);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyInUseException();
        }
    }

    public Nutritionist getById(Long id) {
        return repository.findById(id).orElseThrow();
    }
}
