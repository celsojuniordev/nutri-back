package com.br.nutri.auth;

import com.br.nutri.auth.dto.GoogleLoginRequest;
import com.br.nutri.auth.dto.GoogleLoginResponse;
import com.br.nutri.auth.dto.LoginRequest;
import com.br.nutri.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.substring("Bearer ".length());
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
