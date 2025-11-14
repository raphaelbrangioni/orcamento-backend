package com.example.orcamento.config;

import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final UsuarioRepository usuarioRepository;

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setEmail("raphaelbrangioni@gmail.com");
                admin.setNome("Raphael Brangioni");
                admin.setTenantId("06660607625");
                // Senha: admin123 (BCrypt hash)
                admin.setAtivo(true);
                admin.setAdmin(true);
                admin.setPassword("$2a$10$KElqWlIeyRvdQOf1G9tLiOAS5u/z8fRDmJ5ysFi0a2ToxWGoHEbeW");
                usuarioRepository.save(admin);
                log.info("Usuário admin criado automaticamente no banco de dados.");
            } else {
                log.info("Usuários já existem no banco. Nenhum usuário admin criado.");
            }
        };
    }
}
