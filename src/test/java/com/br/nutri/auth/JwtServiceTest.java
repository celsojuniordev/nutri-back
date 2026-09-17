package com.br.nutri.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.nutri.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-characters";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 3600));

    @Test
    void generatesAndParsesAValidToken() {
        String token = jwtService.generateToken(42L);

        var jws = jwtService.parse(token).orElseThrow();

        assertThat(jwtService.extractNutritionistId(jws)).isEqualTo(42L);
        assertThat(jwtService.extractJti(jws)).isNotBlank();
    }

    @Test
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("1")
                .id("expired-jti")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(jwtService.parse(expiredToken)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithADifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-value-1234".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String tokenWithWrongSignature = Jwts.builder()
                .subject("1")
                .id("some-jti")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.parse(tokenWithWrongSignature)).isEmpty();
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }
}
