package com.example.orcamento.config;

import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final AdminBootstrapProperties adminBootstrapProperties;

    @Bean
    @Profile({"dev", "hml"})
    public CommandLineRunner initAdminUser() {
        return args -> {
            if (!adminBootstrapProperties.isEnabled()) {
                log.info("Bootstrap de admin desabilitado para o ambiente atual.");
                return;
            }

            if (usuarioRepository.count() > 0) {
                log.info("Usuarios ja existem no banco. Nenhum usuario admin criado.");
                return;
            }

            Usuario admin = new Usuario();
            admin.setUsername(adminBootstrapProperties.getUsername());
            admin.setEmail(adminBootstrapProperties.getEmail());
            admin.setNome(adminBootstrapProperties.getNome());
            admin.setTenantId(adminBootstrapProperties.getTenantId());
            admin.setAtivo(true);
            admin.setAdmin(true);
            admin.setPassword(adminBootstrapProperties.getPasswordHash());

            usuarioRepository.save(admin);
            log.info("Usuario admin criado automaticamente no banco de dados.");
        };
    }
}
