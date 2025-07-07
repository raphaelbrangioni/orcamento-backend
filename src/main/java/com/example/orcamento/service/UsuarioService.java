package com.example.orcamento.service;

import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public String authenticate(String username, String password) {
        // Buscar usuário no banco
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Dados Invalidos"));

        // Verifica a senha (agora usando `passwordEncoder.matches()`)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        // Bloqueia login se não validou o token do primeiro login
        if (user.isPrimeiroLogin() || user.getDataPrimeiroLogin() == null) {
            throw new IllegalStateException("Primeiro acesso: valide o token enviado antes de acessar o sistema.");
        }

        // Gera o token JWT incluindo o tenantId
        return jwtUtil.generateToken(username, user.getTenantId());
    }

    public Usuario criarUsuario(String username, String senha, String email, String tenantId) {
        // Verifica se o usuário já existe
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(senha)); // Criptografa a senha
        usuario.setTenantId(tenantId);

        return usuarioRepository.save(usuario);
    }

    public Usuario criarUsuario(String username, String password, String nome, String email, String tenantId, boolean ativo, boolean admin, boolean primeiroLogin, String token) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTenantId(tenantId);
        usuario.setAtivo(ativo);
        usuario.setAdmin(admin);
        usuario.setPrimeiroLogin(primeiroLogin);
        usuario.setToken(token);
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setDataPrimeiroLogin(null);
        Usuario salvo = usuarioRepository.save(usuario);
        // Envia e-mail de boas-vindas com formatação HTML
        String assunto = "Bem-vindo ao Meu Orçamento - Acesse sua nova conta";
        String corpo = String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }" +
            "        .header { background-color: #2563eb; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
            "        .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }" +
            "        .footer { margin-top: 20px; font-size: 12px; color: #666; text-align: center; }" +
            "        .credentials { background-color: #fff; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin: 20px 0; }" +
            "        .highlight { font-weight: bold; color: #2563eb; }" +
            "        .button { display: inline-block; background-color: #2563eb; color: white; text-decoration: none; padding: 10px 20px; border-radius: 5px; margin-top: 15px; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"header\">" +
            "        <h2>Bem-vindo ao Meu Orçamento!</h2>" +
            "    </div>" +
            "    <div class=\"content\">" +
            "        <p>Olá <b>%s</b>,</p>" +
            "        <p>Seu cadastro foi realizado com sucesso! Estamos felizes em tê-lo como usuário do nosso sistema de gestão financeira.</p>" +
            "        <div class=\"credentials\">" +
            "            <p><strong>Suas credenciais de acesso:</strong></p>" +
            "            <p>Usuário: <span class=\"highlight\">%s</span></p>" +
            "            <p>Senha inicial: <span class=\"highlight\">%s</span></p>" +
            "            <p>Token de validação: <span class=\"highlight\">%s</span></p>" +
            "        </div>" +
            "        <p><strong>Próximos passos:</strong></p>" +
            "        <ol>" +
            "            <li>Acesse o sistema em <a href=\"meuorcamentoapp.com.br\">meuorcamentoapp.com.br</a></li>" +
            "            <li>Faça login com seu usuário e senha</li>" +
            "            <li>Quando solicitado, informe o token de validação acima</li>" +
            "            <li>Defina uma nova senha segura</li>" +
            "        </ol>" +
            "        <p>Por motivos de segurança, este token é válido por 24 horas. Após este período, será necessário solicitar um novo token.</p>" +
            "        <p>Caso tenha qualquer dúvida, responda a este email ou entre em contato com o suporte.</p>" +
            "    </div>" +
            "    <div class=\"footer\">" +
            "        <p>Atenciosamente,<br>Equipe Meu Orçamento</p>" +
            "        <p> %d Meu Orçamento - Todos os direitos reservados</p>" +
            "        <p><small>Este é um email automático, por favor não responda.</small></p>" +
            "    </div>" +
            "</body>" +
            "</html>",
            nome, username, password, token, java.time.Year.now().getValue()
        );
        emailService.enviarEmail(email, assunto, corpo);
        return salvo;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obterUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public Usuario obterUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public void atualizarSenha(String username, String novaSenha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        usuario.setPassword(passwordEncoder.encode(novaSenha)); // Criptografa a nova senha
        usuarioRepository.save(usuario);
    }

    public Usuario atualizarUsuario(Long id, Usuario usuarioAtualizado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        usuario.setUsername(usuarioAtualizado.getUsername());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setTenantId(usuarioAtualizado.getTenantId());
        // Só atualiza a senha se vier preenchida
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }
        usuario.setAtivo(usuarioAtualizado.isAtivo());
        usuario.setAdmin(usuarioAtualizado.isAdmin());
        return usuarioRepository.save(usuario);
    }

    public boolean validarTokenPrimeiroLogin(String username, String tokenInformado) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        if (usuario.isPrimeiroLogin() && usuario.getToken() != null && usuario.getToken().equals(tokenInformado)) {
            usuario.setPrimeiroLogin(false);
            usuario.setToken(null); // Remove o token após uso
            usuario.setDataPrimeiroLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }
}
