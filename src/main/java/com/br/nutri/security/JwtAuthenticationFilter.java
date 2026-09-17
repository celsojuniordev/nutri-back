package com.br.nutri.security;

import com.br.nutri.auth.JwtService;
import com.br.nutri.auth.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.parse(token).ifPresent(jws -> {
                String jti = jwtService.extractJti(jws);
                if (!revokedTokenRepository.existsByJti(jti)) {
                    Long nutritionistId = jwtService.extractNutritionistId(jws);
                    var authentication =
                            new UsernamePasswordAuthenticationToken(nutritionistId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }
        filterChain.doFilter(request, response);
    }
}
