package com.br.nutri.auth;

import com.br.nutri.common.GoogleTokenInvalidException;
import com.br.nutri.config.GoogleProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleTokenVerifierService(GoogleProperties properties) {
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(properties.clientId()))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize Google ID token verifier", e);
        }
    }

    GoogleTokenVerifierService(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public GooglePayload verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new GoogleTokenInvalidException("Não foi possível validar o token do Google.");
        }
        if (idToken == null) {
            throw new GoogleTokenInvalidException("Token do Google inválido, expirado ou com assinatura incorreta.");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified == null || !emailVerified) {
            throw new GoogleTokenInvalidException("E-mail da conta Google não verificado.");
        }
        String name = (String) payload.get("name");
        return new GooglePayload(payload.getSubject(), payload.getEmail(), name);
    }

    public record GooglePayload(String subject, String email, String name) {
    }
}
