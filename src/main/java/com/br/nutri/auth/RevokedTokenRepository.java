package com.br.nutri.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);
}
