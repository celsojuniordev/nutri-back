package com.br.nutri.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
