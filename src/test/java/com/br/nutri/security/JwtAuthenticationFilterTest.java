package com.br.nutri.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.nutri.auth.JwtService;
import com.br.nutri.auth.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> jws;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesSecurityContextForValidNonRevokedToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revokedTokenRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parse("valid-token")).thenReturn(Optional.of(jws));
        when(jwtService.extractJti(jws)).thenReturn("jti-1");
        when(revokedTokenRepository.existsByJti("jti-1")).thenReturn(false);
        when(jwtService.extractNutritionistId(jws)).thenReturn(7L);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(7L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenTokenIsRevoked() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revokedTokenRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parse("revoked-token")).thenReturn(Optional.of(jws));
        when(jwtService.extractJti(jws)).thenReturn("jti-2");
        when(revokedTokenRepository.existsByJti("jti-2")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateWhenTokenIsInvalid() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revokedTokenRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parse("invalid-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateWhenAuthorizationHeaderIsMissing() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revokedTokenRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, org.mockito.Mockito.never()).parse(any());
    }
}
