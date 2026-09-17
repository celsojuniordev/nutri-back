package com.br.nutri.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.br.nutri.common.GoogleTokenInvalidException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierServiceTest {

    @Mock
    private GoogleIdTokenVerifier verifier;

    @Mock
    private GoogleIdToken idToken;

    @Mock
    private GoogleIdToken.Payload payload;

    @Test
    void returnsSubjectEmailAndNameForAValidVerifiedToken() throws GeneralSecurityException, IOException {
        when(verifier.verify("valid-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getEmail()).thenReturn("ana@example.com");
        when(payload.getSubject()).thenReturn("google-subject-123");
        when(payload.get("name")).thenReturn("Ana Silva");

        GoogleTokenVerifierService service = new GoogleTokenVerifierService(verifier);
        GoogleTokenVerifierService.GooglePayload result = service.verify("valid-token");

        assertThat(result.subject()).isEqualTo("google-subject-123");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.name()).isEqualTo("Ana Silva");
    }

    @Test
    void rejectsTokenThatFailsSignatureOrIssuerVerification() throws GeneralSecurityException, IOException {
        when(verifier.verify("invalid-token")).thenReturn(null);

        GoogleTokenVerifierService service = new GoogleTokenVerifierService(verifier);

        assertThatThrownBy(() -> service.verify("invalid-token")).isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    void rejectsTokenWithUnverifiedEmail() throws GeneralSecurityException, IOException {
        when(verifier.verify("token-unverified-email")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(false);

        GoogleTokenVerifierService service = new GoogleTokenVerifierService(verifier);

        assertThatThrownBy(() -> service.verify("token-unverified-email"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    void rejectsWhenVerifierThrowsGeneralSecurityException() throws GeneralSecurityException, IOException {
        when(verifier.verify("bad-signature-token")).thenThrow(new GeneralSecurityException("bad signature"));

        GoogleTokenVerifierService service = new GoogleTokenVerifierService(verifier);

        assertThatThrownBy(() -> service.verify("bad-signature-token"))
                .isInstanceOf(GoogleTokenInvalidException.class);
    }

    @Test
    void rejectsExpiredTokenReportedAsIOException() throws GeneralSecurityException, IOException {
        when(verifier.verify("expired-token")).thenThrow(new IOException("token expired"));

        GoogleTokenVerifierService service = new GoogleTokenVerifierService(verifier);

        assertThatThrownBy(() -> service.verify("expired-token")).isInstanceOf(GoogleTokenInvalidException.class);
    }
}
