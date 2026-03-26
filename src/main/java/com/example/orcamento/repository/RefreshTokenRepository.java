package com.example.orcamento.repository;

import com.example.orcamento.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.usuario WHERE rt.token = :token")
    Optional<RefreshToken> findByToken(@Param("token") String token);

    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.usuario WHERE rt.token = :token AND rt.tenantId = :tenantId")
    Optional<RefreshToken> findByTokenAndTenantId(@Param("token") String token, @Param("tenantId") String tenantId);

    @Transactional
    void deleteByToken(String token);

    @Transactional
    void deleteByTokenAndTenantId(String token, String tenantId);

    void deleteByUsuarioId(Long usuarioId);

    void deleteByUsuarioIdAndTenantId(Long usuarioId, String tenantId);
}
