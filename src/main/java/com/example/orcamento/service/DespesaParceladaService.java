package com.example.orcamento.service;

import com.example.orcamento.dto.DespesaParceladaDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.DespesaParcelada;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.ContaCorrenteRepository;
import com.example.orcamento.repository.DespesaParceladaRepository;
import com.example.orcamento.repository.TipoDespesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;



@Service
@RequiredArgsConstructor
@Slf4j
public class DespesaParceladaService {

    private final DespesaParceladaRepository despesaParceladaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;
    private final DespesaService despesaService;

    public Page<DespesaParcelada> listarDespesasParceladas(
            String descricao, Long tipoDespesaId, Pageable pageable) {
        return despesaParceladaRepository.findByFiltros(descricao, tipoDespesaId, pageable);
    }

    @Transactional
    public DespesaParcelada salvarDespesaParcelada(DespesaParceladaDTO dto) {
        // Buscar tipo de despesa
        TipoDespesa tipoDespesa = tipoDespesaRepository.findById(dto.getTipoDespesaId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado"));

        // Criar e salvar a despesa parcelada
        DespesaParcelada despesaParcelada = new DespesaParcelada();
        despesaParcelada.setDescricao(dto.getDescricao());
        despesaParcelada.setValorTotal(dto.getValorTotal());
        despesaParcelada.setNumeroParcelas(dto.getNumeroParcelas());
        despesaParcelada.setDataInicial(dto.getDataInicial());
        despesaParcelada.setMesPrimeiraParcela(dto.getMesPrimeiraParcela());
        despesaParcelada.setTipoDespesa(tipoDespesa);
        despesaParcelada.setProprietario(dto.getProprietario());
        despesaParcelada.setDetalhes(dto.getDetalhes());
        despesaParcelada.setClassificacao(dto.getClassificacao());
        despesaParcelada.setVariabilidade(dto.getVariabilidade());

        DespesaParcelada despesaSalva = despesaParceladaRepository.save(despesaParcelada);

        // Gerar as parcelas como despesas individuais
        gerarParcelas(despesaSalva);

        return despesaSalva;
    }

    @Transactional
    public DespesaParcelada atualizarDespesaParcelada(Long id, DespesaParceladaDTO dto) {
        DespesaParcelada despesaExistente = despesaParceladaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa parcelada não encontrada"));

        // Buscar tipo de despesa
        TipoDespesa tipoDespesa = tipoDespesaRepository.findById(dto.getTipoDespesaId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado"));

        // Atualizar a despesa parcelada
        despesaExistente.setDescricao(dto.getDescricao());
        despesaExistente.setValorTotal(dto.getValorTotal());
        despesaExistente.setNumeroParcelas(dto.getNumeroParcelas());
        despesaExistente.setDataInicial(dto.getDataInicial());
        despesaExistente.setMesPrimeiraParcela(dto.getMesPrimeiraParcela());
        despesaExistente.setTipoDespesa(tipoDespesa);
        despesaExistente.setProprietario(dto.getProprietario());
        despesaExistente.setDetalhes(dto.getDetalhes());

        return despesaParceladaRepository.save(despesaExistente);
    }

//    @Transactional
//    public void excluirDespesaParcelada(Long id) {
//        // Aqui você pode implementar a lógica para excluir também as despesas (parcelas) associadas
//        despesaParceladaRepository.deleteById(id);
//    }

    @Transactional
    public void excluirDespesaParcelada(Long id) {
        // Primeiro, verificamos se a despesa parcelada existe
        DespesaParcelada despesaParcelada = despesaParceladaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa parcelada não encontrada"));

        // Excluir todas as parcelas (despesas) associadas a esta despesa parcelada
        despesaService.excluirDespesasPorDespesaParceladaId(id);

        // Por fim, excluímos a despesa parcelada em si
        despesaParceladaRepository.deleteById(id);

        log.info("Despesa parcelada ID {} e todas as suas parcelas foram excluídas com sucesso", id);
    }

    private void gerarParcelas(DespesaParcelada despesaParcelada) {
        int numeroParcelas = despesaParcelada.getNumeroParcelas();
        BigDecimal valorParcela = despesaParcelada.getValorTotal()
                .divide(BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.HALF_UP);

        // Calcular o valor da última parcela para compensar arredondamentos
        BigDecimal valorTotalParcelas = valorParcela.multiply(BigDecimal.valueOf(numeroParcelas - 1));
        BigDecimal valorUltimaParcela = despesaParcelada.getValorTotal().subtract(valorTotalParcelas);

        // Usar o ano da data inicial informada pelo usuário
        LocalDate dataInicial = despesaParcelada.getDataInicial();
        int anoInicial = dataInicial.getYear();
        Month mesPrimeiraParcela = despesaParcelada.getMesPrimeiraParcela();

        List<Despesa> parcelas = new ArrayList<>();

        for (int i = 0; i < numeroParcelas; i++) {
            // Calcular o mês e ano desta parcela
            Month mesParcela = Month.of(((mesPrimeiraParcela.getValue() - 1 + i) % 12) + 1);
            int anoParcela = anoInicial + ((mesPrimeiraParcela.getValue() - 1 + i) / 12);

            // Criar a data de vencimento (usando o dia da data inicial)
            int diaVencimento = dataInicial.getDayOfMonth();
            LocalDate dataVencimento = LocalDate.of(anoParcela, mesParcela, diaVencimento);

            // Criar a despesa (parcela)
            Despesa parcela = new Despesa();

            // Usar os nomes de campos corretos conforme seu modelo
            parcela.setNome(despesaParcelada.getDescricao() + " - Parcela " + (i + 1) + "/" + numeroParcelas);

            // Definir o valor da parcela (a última pode ser diferente para compensar arredondamentos)
            if (i == numeroParcelas - 1) {
                parcela.setValorPrevisto(valorUltimaParcela);
            } else {
                parcela.setValorPrevisto(valorParcela);
            }

            parcela.setDataVencimento(dataVencimento);
            parcela.setTipo(despesaParcelada.getTipoDespesa());
            parcela.setDetalhes(despesaParcelada.getDetalhes());
            parcela.setDespesaParceladaId(despesaParcelada.getId());
            parcela.setNome(despesaParcelada.getDescricao() + " - Parcela " + (i + 1) + "/" + numeroParcelas);
            parcela.setClassificacao(despesaParcelada.getClassificacao());
            parcela.setVariabilidade(despesaParcelada.getVariabilidade());


            // Salvar a parcela
            despesaService.salvarDespesa(parcela);
            parcelas.add(parcela);
        }
    }
}