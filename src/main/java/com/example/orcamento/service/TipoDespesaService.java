package com.example.orcamento.service;

import com.example.orcamento.dto.TipoDespesaRequestDTO;
import com.example.orcamento.dto.TipoDespesaResponseDTO;
import com.example.orcamento.dto.TipoDespesaCategoriaRequestDTO;
import com.example.orcamento.dto.TipoDespesaCategoriaResponseDTO;
import com.example.orcamento.dto.TipoDespesaSubcategoriaRequestDTO;
import com.example.orcamento.dto.TipoDespesaSubcategoriaResponseDTO;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.CategoriaDespesaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.repository.TipoDespesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoDespesaService {

    private final TipoDespesaRepository tipoDespesaRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;

    public List<TipoDespesa> listarTipos() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return tipoDespesaRepository.findByTenantId(tenantId);
    }

    public TipoDespesa cadastrarTipo(TipoDespesa tipo) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tipoDespesaRepository.existsByNomeAndTenantId(tipo.getNome(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de despesa já existe!");
        }
        tipo.setTenantId(tenantId);
        return tipoDespesaRepository.save(tipo);
    }

    @Transactional
    public TipoDespesa cadastrarTipoDTO(TipoDespesaRequestDTO dto) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tipoDespesaRepository.existsByNomeAndTenantId(dto.getNome(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de despesa já existe!");
        }
        SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findById(dto.getSubcategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcategoria não encontrada"));
        TipoDespesa tipo = new TipoDespesa();
        tipo.setNome(dto.getNome());
        tipo.setTenantId(tenantId);
        tipo.setSubcategoria(subcategoria);
        return tipoDespesaRepository.save(tipo);
    }

    public void excluirTipo(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        tipoDespesaRepository.deleteByIdAndTenantId(id, tenantId);
    }

    public TipoDespesa atualizarTipoDespesa(Long id, TipoDespesa tipoDespesaAtualizado) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        TipoDespesa tipoDespesaExistente = tipoDespesaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado para o ID: " + id + " do tenant atual."));

        // Atualiza os campos permitidos
        tipoDespesaExistente.setNome(tipoDespesaAtualizado.getNome());

        // Salva as alterações
        return tipoDespesaRepository.save(tipoDespesaExistente);
    }

    @Transactional
    public TipoDespesa atualizarTipoDespesaDTO(Long id, TipoDespesaRequestDTO dto) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        TipoDespesa tipo = tipoDespesaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado para o ID: " + id + " do tenant atual."));
        tipo.setNome(dto.getNome());
        if (dto.getSubcategoriaId() != null) {
            SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findById(dto.getSubcategoriaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcategoria não encontrada"));
            tipo.setSubcategoria(subcategoria);
        } else {
            tipo.setSubcategoria(null);
        }
        return tipoDespesaRepository.save(tipo);
    }

    public TipoDespesaResponseDTO toResponseDTO(TipoDespesa tipo) {
        TipoDespesaResponseDTO dto = new TipoDespesaResponseDTO();
        dto.setId(tipo.getId());
        dto.setNome(tipo.getNome());
        dto.setTenantId(tipo.getTenantId());
        if (tipo.getSubcategoria() != null) {
            TipoDespesaResponseDTO.SubcategoriaDTO subDTO = new TipoDespesaResponseDTO.SubcategoriaDTO();
            subDTO.setId(tipo.getSubcategoria().getId());
            subDTO.setNome(tipo.getSubcategoria().getNome());
            dto.setSubcategoria(subDTO);
            if (tipo.getSubcategoria().getCategoria() != null) {
                TipoDespesaResponseDTO.CategoriaDTO catDTO = new TipoDespesaResponseDTO.CategoriaDTO();
                catDTO.setId(tipo.getSubcategoria().getCategoria().getId());
                catDTO.setNome(tipo.getSubcategoria().getCategoria().getNome());
                dto.setCategoria(catDTO);
            }
        }
        return dto;
    }

    public List<CategoriaDespesa> listarCategoriasPorTenant(String tenantId) {
        return categoriaDespesaRepository.findByTenantId(tenantId);
    }

    public List<TipoDespesaCategoriaResponseDTO> listarCategoriasComSubcategoriasPorTenant(String tenantId) {
        List<CategoriaDespesa> categorias = categoriaDespesaRepository.findByTenantIdWithSubcategorias(tenantId);
        return categorias.stream().map(cat -> {
            TipoDespesaCategoriaResponseDTO dto = new TipoDespesaCategoriaResponseDTO();
            dto.setId(cat.getId());
            dto.setNome(cat.getNome());
            // dto.setIcone(cat.getIcone()); // descomente se possuir campo icone
            List<TipoDespesaSubcategoriaResponseDTO> subDtos = (cat.getSubcategorias() != null)
                ? cat.getSubcategorias().stream().map(sub -> {
                    TipoDespesaSubcategoriaResponseDTO subDto = new TipoDespesaSubcategoriaResponseDTO();
                    subDto.setId(sub.getId());
                    subDto.setNome(sub.getNome());
                    // NÃO setar categoria aqui para evitar recursividade infinita
                    return subDto;
                }).toList() : List.of();
            dto.setSubcategorias(subDtos);
            return dto;
        }).toList();
    }

    @Transactional
    public TipoDespesaCategoriaResponseDTO criarCategoriaComSubcategorias(TipoDespesaCategoriaRequestDTO dto, String tenantId) {
        if (categoriaDespesaRepository.existsByNomeAndTenantId(dto.getNome(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria já existe!");
        }
        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setNome(dto.getNome());
        categoria.setTenantId(tenantId);
        // categoria.setIcone(dto.getIcone()); // descomente se possuir campo icone

        List<SubcategoriaDespesa> subcategorias = null;
        if (dto.getSubcategorias() != null) {
            subcategorias = dto.getSubcategorias().stream().map(subDto -> {
                SubcategoriaDespesa sub = new SubcategoriaDespesa();
                sub.setNome(subDto.getNome());
                sub.setCategoria(categoria);
                return sub;
            }).toList();
            categoria.setSubcategorias(subcategorias);
        }
        CategoriaDespesa salva = categoriaDespesaRepository.save(categoria);

        TipoDespesaCategoriaResponseDTO resposta = new TipoDespesaCategoriaResponseDTO();
        resposta.setId(salva.getId());
        resposta.setNome(salva.getNome());
        // resposta.setIcone(salva.getIcone()); // descomente se possuir campo icone
        if (salva.getSubcategorias() != null) {
            List<TipoDespesaSubcategoriaResponseDTO> subDtos = salva.getSubcategorias().stream().map(sub -> {
                TipoDespesaSubcategoriaResponseDTO subResp = new TipoDespesaSubcategoriaResponseDTO();
                subResp.setId(sub.getId());
                subResp.setNome(sub.getNome());
                return subResp;
            }).toList();
            resposta.setSubcategorias(subDtos);
        }
        return resposta;
    }
}
