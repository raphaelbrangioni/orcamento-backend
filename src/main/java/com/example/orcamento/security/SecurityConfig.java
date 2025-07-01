package com.example.orcamento.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .anonymous(anonymous -> anonymous.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/tipos-despesa/**").authenticated()
                        .requestMatchers("/api/v1/despesas/**").authenticated()
                        .requestMatchers("/api/v1/cartoes-credito/**").authenticated()
                        .requestMatchers("/api/v1/lancamentos-cartao/**").authenticated()
                        .requestMatchers("/api/v1/lancamentos-cartao/{id}/pago-por-terceiro/**").authenticated()
                        .requestMatchers("/api/v1/receitas").authenticated()
                        .requestMatchers("/api/v1/receitas/**").authenticated()
                        .requestMatchers("/api/v1/receitas/{id}/efetivar").authenticated()
                        .requestMatchers("/api/v1/relatorios").authenticated()
                        .requestMatchers("/api/v1/relatorios/**").authenticated()
                        .requestMatchers("/api/v1/contas-corrente/**").authenticated()
                        .requestMatchers("/api/v1/movimentacoes/**").authenticated()
                        .requestMatchers("/api/v1/salarios-previstos/**").authenticated()
                        .requestMatchers("/api/v1/limites").authenticated()
                        .requestMatchers("/api/v1/limites/**").authenticated()
                        .requestMatchers("/api/v1/limites/{id}").authenticated()
                        .requestMatchers("/api/v1/alertas/limites/**").authenticated()
                        .requestMatchers("/api/v1/metas-economia/**").authenticated()
                        .requestMatchers("/api/v1/despesas/analise/**").authenticated()
                        .requestMatchers("/api/v1/cartoes/analise/**").authenticated()
                        .requestMatchers("/api/v1/compras/**").authenticated()
                        .requestMatchers("api/v1/despesas/ano/**").authenticated()
                        .requestMatchers("/api/v1/despesas-parceladas/**").authenticated()
                        .requestMatchers("/api/v1/transacoes/**").authenticated()
                        .requestMatchers("/api/v1/transacoes/filtrar-dinamico").authenticated()
                        .requestMatchers("/api/v1/pdf/**").authenticated()
                        .requestMatchers("/api/v1/configuracoes/**").authenticated()
                        .requestMatchers("/api/v1/api/v1/auth/trocar-senha").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .anyRequest().denyAll()

                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                           log.error("Erro de autenticação: " + authEx.getMessage());
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.getWriter().write("Unauthorized: " + authEx.getMessage());
                        })
                        .accessDeniedHandler((req, res, accessEx) -> {
                            log.error("Acesso negado: " + accessEx.getMessage());
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.getWriter().write("Forbidden: " + accessEx.getMessage());
                        }));

        System.out.println("SecurityFilterChain configurado com sucesso");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); // Permite qualquer origem
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false); // Importante: false se usar allowedOrigin "*"
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}