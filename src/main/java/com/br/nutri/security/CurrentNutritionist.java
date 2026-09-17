package com.br.nutri.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentNutritionist {

    private CurrentNutritionist() {
    }

    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new IllegalStateException("No authenticated nutritionist in the security context");
        }
        return id;
    }
}
