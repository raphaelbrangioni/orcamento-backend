package com.example.orcamento.repository;

import com.example.orcamento.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByToken(String token);

    void deleteByUsuarioId(Long usuarioId);
}
