package com.example.orcamento.service;

import com.example.orcamento.dto.AcessoUsuarioDTO;
import com.example.orcamento.dto.UsuarioAcessosDTO;
import com.example.orcamento.model.AcessoUsuario;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.AcessoUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcessoUsuarioService {
    private final AcessoUsuarioRepository acessoUsuarioRepository;

    public AcessoUsuario registrarLogin(Usuario usuario, String ipOrigem, String tenantId) {
        AcessoUsuario acesso = new AcessoUsuario();
        acesso.setUsuario(usuario);
        acesso.setIpOrigem(ipOrigem);
        acesso.setTenantId(tenantId);
        acesso.setDataHoraLogin(LocalDateTime.now());
        return acessoUsuarioRepository.save(acesso);
    }

    public void registrarLogout(Long acessoId) {
        acessoUsuarioRepository.findById(acessoId).ifPresent(acesso -> {
            acesso.setDataHoraLogout(LocalDateTime.now());
            acessoUsuarioRepository.save(acesso);
        });
    }

    public List<UsuarioAcessosDTO> listarAcessosPorUsuario(List<Usuario> usuarios) {
        return usuarios.stream().map(usuario -> {
            UsuarioAcessosDTO dto = new UsuarioAcessosDTO();
            dto.setUsuarioId(usuario.getId());
            dto.setUsername(usuario.getUsername());
            dto.setNome(usuario.getNome());
            List<AcessoUsuarioDTO> acessos = usuario.getAcessos() != null ? usuario.getAcessos().stream().map(acesso -> {
                AcessoUsuarioDTO acessoDTO = new AcessoUsuarioDTO();
                acessoDTO.setId(acesso.getId());
                acessoDTO.setDataHoraLogin(acesso.getDataHoraLogin());
                acessoDTO.setDataHoraLogout(acesso.getDataHoraLogout());
                acessoDTO.setIpOrigem(acesso.getIpOrigem());
                acessoDTO.setTenantId(acesso.getTenantId());
                return acessoDTO;
            }).collect(Collectors.toList()) : List.of();
            dto.setAcessos(acessos);
            return dto;
        }).collect(Collectors.toList());
    }
}
