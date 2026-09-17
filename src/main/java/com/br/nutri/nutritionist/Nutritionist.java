package com.br.nutri.nutritionist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "nutritionists")
public class Nutritionist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String company;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Nutritionist() {
    }

    public Nutritionist(String name, String company, String email, String passwordHash, String googleSubject) {
        if (passwordHash == null && googleSubject == null) {
            throw new IllegalArgumentException(
                    "A nutritionist must have either a password or a linked Google account");
        }
        this.name = name;
        this.company = company;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
