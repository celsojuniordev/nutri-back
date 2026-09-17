package com.br.nutri.auth;

import com.br.nutri.auth.dto.GoogleLoginRequest;
import com.br.nutri.auth.dto.GoogleLoginResponse;
import com.br.nutri.auth.dto.LoginRequest;
import com.br.nutri.common.InvalidCredentialsException;
import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final NutritionistRepository nutritionistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final RevokedTokenRepository revokedTokenRepository;

    public AuthService(
            NutritionistRepository nutritionistRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifierService googleTokenVerifierService,
            RevokedTokenRepository revokedTokenRepository) {
        this.nutritionistRepository = nutritionistRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public String login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Nutritionist nutritionist = nutritionistRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (nutritionist.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), nutritionist.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateToken(nutritionist.getId());
    }

    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifierService.GooglePayload payload = googleTokenVerifierService.verify(request.idToken());
        String email = payload.email().trim().toLowerCase(Locale.ROOT);

        Optional<Nutritionist> bySubject = nutritionistRepository.findByGoogleSubject(payload.subject());
        if (bySubject.isPresent()) {
            return new GoogleLoginResponse(jwtService.generateToken(bySubject.get().getId()), false);
        }

        Optional<Nutritionist> byEmail = nutritionistRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            Nutritionist existing = byEmail.get();
            if (existing.getGoogleSubject() == null) {
                existing.setGoogleSubject(payload.subject());
                nutritionistRepository.save(existing);
            }
            return new GoogleLoginResponse(jwtService.generateToken(existing.getId()), false);
        }

        Nutritionist created = new Nutritionist(payload.name(), null, email, null, payload.subject());
        try {
            nutritionistRepository.saveAndFlush(created);
            return new GoogleLoginResponse(jwtService.generateToken(created.getId()), true);
        } catch (DataIntegrityViolationException e) {
            Nutritionist winner = nutritionistRepository
                    .findByEmailIgnoreCase(email)
                    .orElseThrow(() -> e);
            return new GoogleLoginResponse(jwtService.generateToken(winner.getId()), false);
        }
    }

    public void logout(String token) {
        jwtService.parse(token).ifPresent(jws -> {
            String jti = jwtService.extractJti(jws);
            if (!revokedTokenRepository.existsByJti(jti)) {
                revokedTokenRepository.save(new RevokedToken(jti, jwtService.extractExpiration(jws)));
            }
        });
    }
}
