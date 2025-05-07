// src/main/java/com/example/orcamento/service/CompraService.java
package com.example.orcamento.service;

import com.example.orcamento.model.Compra;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.CompraRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.TipoDespesaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraService {
    private final CompraRepository compraRepository;
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final TipoDespesaRepository tipoDespesaRepository;

    @Transactional
    public Compra cadastrarCompraParcelada(Compra compra, String mesPrimeiraParcela, Integer numeroParcelas) {
        log.info("Cadastrando compra parcelada: {}", compra);

        // Salva a compra
        Compra compraSalva = compraRepository.save(compra);

        // Gera e salva as parcelas
        List<LancamentoCartao> parcelas = gerarParcelas(compraSalva, mesPrimeiraParcela, numeroParcelas);
        parcelas.forEach(parcela -> parcela.setCompra(compraSalva));
        lancamentoCartaoRepository.saveAll(parcelas);

        return compraSalva;
    }

    private List<LancamentoCartao> gerarParcelas(Compra compra, String mesPrimeiraParcela, Integer numeroParcelas) {
        List<LancamentoCartao> parcelas = new ArrayList<>();
        String[] meses = {
                "JANEIRO", "FEVEREIRO", "MARCO", "ABRIL", "MAIO", "JUNHO",
                "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
        };

        int indiceMesInicial = -1;
        for (int i = 0; i < meses.length; i++) {
            if (meses[i].equals(mesPrimeiraParcela)) {
                indiceMesInicial = i;
                break;
            }
        }
        if (indiceMesInicial == -1) {
            throw new IllegalArgumentException("Mês da primeira parcela inválido: " + mesPrimeiraParcela);
        }
        if (numeroParcelas <= 0) {
            throw new IllegalArgumentException("Número de parcelas deve ser maior que zero");
        }

        int anoInicial = compra.getDataCompra().getYear();
        BigDecimal valorTotal = compra.getValorTotal();
        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(numeroParcelas), 2, BigDecimal.ROUND_HALF_UP);

        for (int i = 0; i < numeroParcelas; i++) {
            int mesIndex = (indiceMesInicial + i) % 12;
            int anosAdicionais = (indiceMesInicial + i) / 12;
            String mesAnoFatura = meses[mesIndex] + "/" + (anoInicial + anosAdicionais);

            LancamentoCartao lancamento = LancamentoCartao.builder()
                    .descricao(compra.getDescricao() + " - Parcela " + (i + 1) + "/" + numeroParcelas)
                    .valorTotal(valorParcela)
                    .parcelaAtual(i + 1)
                    .totalParcelas(numeroParcelas)
                    .dataCompra(compra.getDataCompra())
                    .cartaoCredito(compra.getCartaoCredito())
                    .tipoDespesa(compra.getTipoDespesa())
                    .proprietario(compra.getProprietario())
                    .detalhes(compra.getDetalhes())
                    .mesAnoFatura(mesAnoFatura)
                    .dataRegistro(LocalDateTime.now())
                    .classificacao(compra.getClassificacao())
                    .variabilidade(compra.getVariabilidade())
                    .build();

            parcelas.add(lancamento);
        }
        return parcelas;
    }
    public List<Compra> listarUltimasCompras(int quantidade) {
        return compraRepository.findAll(PageRequest.of(0, quantidade, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }

    @Transactional
    public Compra editarCompra(Long id, Compra compraAtualizada, String mesPrimeiraParcela, Integer numeroParcelas) {
        log.info("Editando compra ID {}: {}", id, compraAtualizada);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compra com ID " + id + " não encontrada."));

        compra.setDescricao(compraAtualizada.getDescricao());
        compra.setValorTotal(compraAtualizada.getValorTotal());
        compra.setNumeroParcelas(compraAtualizada.getNumeroParcelas());
        compra.setDataCompra(compraAtualizada.getDataCompra());
        compra.setCartaoCredito(compraAtualizada.getCartaoCredito());

        // Validação e busca do TipoDespesa
        if (compraAtualizada.getTipoDespesa() == null || compraAtualizada.getTipoDespesa().getId() == null) {
            throw new IllegalArgumentException("O tipo de despesa é obrigatório.");
        }
        TipoDespesa tipoDespesa = tipoDespesaRepository.findById(compraAtualizada.getTipoDespesa().getId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de despesa com ID " + compraAtualizada.getTipoDespesa().getId() + " não encontrado."));
        compra.setTipoDespesa(tipoDespesa);

        compra.setProprietario(compraAtualizada.getProprietario());
        compra.setDetalhes(compraAtualizada.getDetalhes());

        // Remove as parcelas antigas e gera novas
        lancamentoCartaoRepository.deleteByCompraId(id);
        List<LancamentoCartao> novasParcelas = gerarParcelas(compra, mesPrimeiraParcela, numeroParcelas);
        novasParcelas.forEach(parcela -> parcela.setCompra(compra));
        lancamentoCartaoRepository.saveAll(novasParcelas);

        return compraRepository.save(compra);
    }

    @Transactional
    public void excluirCompra(Long id) {
        log.info("Excluindo compra ID {}", id);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compra com ID " + id + " não encontrada."));
        lancamentoCartaoRepository.deleteByCompraId(id); // Remove as parcelas associadas
        compraRepository.delete(compra);
    }

    public Page<Compra> listarCompras(int page, int size, String descricao, Long cartaoId, Long tipoDespesaId) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (StringUtils.hasText(descricao) || cartaoId != null || tipoDespesaId != null) {
            return compraRepository.findByFilters(descricao, cartaoId, tipoDespesaId, pageRequest);
        }
        return compraRepository.findAll(pageRequest);
    }
}