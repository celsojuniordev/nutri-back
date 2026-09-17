package com.br.nutri.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.nutri.auth.dto.GoogleLoginRequest;
import com.br.nutri.auth.dto.LoginRequest;
import com.br.nutri.common.GoogleTokenInvalidException;
import com.br.nutri.common.InvalidCredentialsException;
import com.br.nutri.nutritionist.Nutritionist;
import com.br.nutri.nutritionist.NutritionistRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private NutritionistRepository nutritionistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                nutritionistRepository,
                passwordEncoder,
                jwtService,
                googleTokenVerifierService,
                revokedTokenRepository);
    }

    @Test
    void issuesTokenForCorrectCredentials() {
        Nutritionist nutritionist = new Nutritionist("Ana Silva", null, "ana@example.com", "hashed", null);
        when(nutritionistRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(nutritionist));
        when(passwordEncoder.matches("Senha123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(nutritionist.getId())).thenReturn("jwt-token");

        String token = authService.login(new LoginRequest("Ana@Example.com", "Senha123"));

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void rejectsUnknownEmailWithoutRevealingWhetherItExists() {
        when(nutritionistRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "Senha123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWrongPassword() {
        Nutritionist nutritionist = new Nutritionist("Ana Silva", null, "ana@example.com", "hashed", null);
        when(nutritionistRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(nutritionist));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginForAccountWithoutLocalPassword() {
        Nutritionist googleOnlyAccount =
                new Nutritionist("Ana Silva", null, "ana@example.com", null, "google-sub-123");
        when(nutritionistRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(googleOnlyAccount));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@example.com", "AnyPassword1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void createsNewAccountWhenNoNutritionistMatchesGoogleTokenSubjectOrEmail() {
        var payload = new GoogleTokenVerifierService.GooglePayload("google-sub-1", "new@example.com", "Ana Silva");
        when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
        when(nutritionistRepository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(nutritionistRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(nutritionistRepository.saveAndFlush(any(Nutritionist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(null)).thenReturn("jwt-token");

        var response = authService.loginWithGoogle(new GoogleLoginRequest("valid-token"));

        assertThat(response.accountCreated()).isTrue();
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void linksExistingAccountByEmailWithoutOverwritingProfile() {
        var payload =
                new GoogleTokenVerifierService.GooglePayload("google-sub-2", "existing@example.com", "Nome Google");
        Nutritionist existing =
                new Nutritionist("Nome Original", "Empresa Original", "existing@example.com", "hash", null);
        when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
        when(nutritionistRepository.findByGoogleSubject("google-sub-2")).thenReturn(Optional.empty());
        when(nutritionistRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing.getId())).thenReturn("jwt-token");

        var response = authService.loginWithGoogle(new GoogleLoginRequest("valid-token"));

        assertThat(response.accountCreated()).isFalse();
        assertThat(existing.getGoogleSubject()).isEqualTo("google-sub-2");
        assertThat(existing.getName()).isEqualTo("Nome Original");
        assertThat(existing.getCompany()).isEqualTo("Empresa Original");
        verify(nutritionistRepository).save(existing);
    }

    @Test
    void authenticatesExistingAccountFoundByGoogleSubjectWithoutTouchingIt() {
        var payload =
                new GoogleTokenVerifierService.GooglePayload("google-sub-3", "linked@example.com", "Ana Silva");
        Nutritionist linked = new Nutritionist("Ana Silva", null, "linked@example.com", null, "google-sub-3");
        when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
        when(nutritionistRepository.findByGoogleSubject("google-sub-3")).thenReturn(Optional.of(linked));
        when(jwtService.generateToken(linked.getId())).thenReturn("jwt-token");

        var response = authService.loginWithGoogle(new GoogleLoginRequest("valid-token"));

        assertThat(response.accountCreated()).isFalse();
        verify(nutritionistRepository, never()).save(any());
    }

    @Test
    void propagatesGoogleTokenInvalidExceptionFromVerifier() {
        when(googleTokenVerifierService.verify("bad-token"))
                .thenThrow(new GoogleTokenInvalidException("token inválido"));

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("bad-token")))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    void resolvesConcurrentDuplicateCreationByAuthenticatingTheWinningAccount() {
        var payload = new GoogleTokenVerifierService.GooglePayload("google-sub-4", "race@example.com", "Ana Silva");
        Nutritionist winner = new Nutritionist("Ana Silva", null, "race@example.com", null, "google-sub-4-other");
        when(googleTokenVerifierService.verify("valid-token")).thenReturn(payload);
        when(nutritionistRepository.findByGoogleSubject("google-sub-4")).thenReturn(Optional.empty());
        when(nutritionistRepository.findByEmailIgnoreCase("race@example.com"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(nutritionistRepository.saveAndFlush(any(Nutritionist.class)))
                .thenThrow(new DataIntegrityViolationException("uk_nutritionists_email"));
        when(jwtService.generateToken(winner.getId())).thenReturn("jwt-token");

        var response = authService.loginWithGoogle(new GoogleLoginRequest("valid-token"));

        assertThat(response.accountCreated()).isFalse();
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void logoutRevokesTheTokenWhenNotAlreadyRevoked() {
        var jws = org.mockito.Mockito.mock(io.jsonwebtoken.Jws.class);
        when(jwtService.parse("some-token")).thenReturn(Optional.of(jws));
        when(jwtService.extractJti(jws)).thenReturn("jti-1");
        when(revokedTokenRepository.existsByJti("jti-1")).thenReturn(false);
        var expiresAt = java.time.Instant.now().plusSeconds(3600);
        when(jwtService.extractExpiration(jws)).thenReturn(expiresAt);

        authService.logout("some-token");

        verify(revokedTokenRepository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        revoked -> revoked.getJti().equals("jti-1") && revoked.getExpiresAt().equals(expiresAt)));
    }

    @Test
    void logoutDoesNotDuplicateAnAlreadyRevokedToken() {
        var jws = org.mockito.Mockito.mock(io.jsonwebtoken.Jws.class);
        when(jwtService.parse("some-token")).thenReturn(Optional.of(jws));
        when(jwtService.extractJti(jws)).thenReturn("jti-1");
        when(revokedTokenRepository.existsByJti("jti-1")).thenReturn(true);

        authService.logout("some-token");

        verify(revokedTokenRepository, never()).save(any());
    }
}
