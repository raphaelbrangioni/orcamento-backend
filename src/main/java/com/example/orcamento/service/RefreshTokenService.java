package com.example.orcamento.service;

import com.example.orcamento.model.RefreshToken;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration.ms:604800000}") // 7 dias padrão
    private long refreshExpirationMs;

    public RefreshToken createRefreshToken(Usuario usuario) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(LocalDateTime.now());
    }

    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    public void deleteByUsuarioId(Long usuarioId) {
        refreshTokenRepository.deleteByUsuarioId(usuarioId);
    }
}
