package com.br.nutri.auth;

import com.br.nutri.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.expirationSeconds();
    }

    public String generateToken(Long nutritionistId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(nutritionistId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    public Optional<Jws<Claims>> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Long extractNutritionistId(Jws<Claims> jws) {
        return Long.valueOf(jws.getPayload().getSubject());
    }

    public String extractJti(Jws<Claims> jws) {
        return jws.getPayload().getId();
    }

    public Instant extractExpiration(Jws<Claims> jws) {
        return jws.getPayload().getExpiration().toInstant();
    }
}
