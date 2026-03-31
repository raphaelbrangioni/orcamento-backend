package com.example.orcamento.service;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxRelatorioDTO;
import com.example.orcamento.model.ConciliacaoOfxProcessamento;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.ConciliacaoOfxProcessamentoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliacaoOfxServiceTest {

    @Mock
    private ContaCorrenteService contaCorrenteService;
    @Mock
    private ConciliacaoOfxProcessamentoRepository conciliacaoOfxProcessamentoRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ReceitaRepository receitaRepository;
    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @InjectMocks
    private ConciliacaoOfxService conciliacaoOfxService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void conciliarDeveAceitarOfxComTagsFechadasNaMesmaLinha() {
        TenantContext.setTenantId("tenantA");
        ContaCorrente contaCorrente = new ContaCorrente(2L, "0001", "10962115", "077", "Inter", BigDecimal.ZERO, 1L, "tenantA", true);
        when(contaCorrenteService.buscarPorId(2L)).thenReturn(Optional.of(contaCorrente));
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(receitaRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(movimentacaoRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(conciliacaoOfxProcessamentoRepository.findTopByTenantIdAndContaCorrenteIdAndHashArquivoOrderByProcessadoEmDesc(eq("tenantA"), eq(2L), any()))
                .thenReturn(Optional.empty());

        String ofx = """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102

                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                <STMTRS>
                <BANKACCTFROM>
                <BANKID>077</BANKID>
                <ACCTID>10962115</ACCTID>
                </BANKACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20260301</DTSTART>
                <DTEND>20260330</DTEND>
                <STMTTRN>
                <DTPOSTED>20260310</DTPOSTED>
                <TRNAMT>-441.93</TRNAMT>
                <FITID>202603100771</FITID>
                <MEMO>Pagamento efetuado: "Debito Automatico Fatura Cartao Inter"</MEMO>
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20260312</DTPOSTED>
                <TRNAMT>1300.00</TRNAMT>
                <FITID>202603120773</FITID>
                <MEMO>Credito liberado: "Pix no credito"</MEMO>
                </STMTTRN>
                </BANKTRANLIST>
                </STMTRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inter.ofx",
                "application/x-ofx",
                ofx.getBytes(StandardCharsets.ISO_8859_1)
        );

        ConciliacaoOfxRelatorioDTO relatorio = conciliacaoOfxService.conciliar(
                2L,
                2,
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                file
        );

        assertThat(relatorio.getBancoIdOfx()).isEqualTo("077");
        assertThat(relatorio.getContaIdOfx()).isEqualTo("10962115");
        assertThat(relatorio.getPeriodoInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(relatorio.getPeriodoFim()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(relatorio.getBancoSemPagamento()).hasSize(1);
        assertThat(relatorio.getBancoSemPagamento().get(0).getFitId()).isEqualTo("202603100771");
        assertThat(relatorio.getBancoSemReceita()).hasSize(1);
        assertThat(relatorio.getBancoSemReceita().get(0).getFitId()).isEqualTo("202603120773");
        verify(conciliacaoOfxProcessamentoRepository).save(any());
    }

    @Test
    void conciliarDeveIdentificarTransferenciaPixDoInterSemDependerDeMemoTransfer() {
        TenantContext.setTenantId("tenantA");
        ContaCorrente contaCorrente = new ContaCorrente(2L, "0001", "10962115", "077", "Inter", BigDecimal.ZERO, 1L, "tenantA", true);
        when(contaCorrenteService.buscarPorId(2L)).thenReturn(Optional.of(contaCorrente));
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(receitaRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(conciliacaoOfxProcessamentoRepository.findTopByTenantIdAndContaCorrenteIdAndHashArquivoOrderByProcessadoEmDesc(eq("tenantA"), eq(2L), any()))
                .thenReturn(Optional.empty());

        Movimentacao transferenciaSaida = Movimentacao.builder()
                .id(777L)
                .tenantId("tenantA")
                .tipo(TipoMovimentacao.SAIDA)
                .valor(new BigDecimal("900.00"))
                .dataRecebimento(LocalDate.of(2026, 3, 20))
                .transferenciaId("tx-900")
                .contaCorrente(contaCorrente)
                .build();

        when(movimentacaoRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(transferenciaSaida));

        String ofx = """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102

                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                <STMTRS>
                <BANKACCTFROM>
                <BANKID>077</BANKID>
                <ACCTID>10962115</ACCTID>
                </BANKACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20260301</DTSTART>
                <DTEND>20260330</DTEND>
                <STMTTRN>
                <DTPOSTED>20260320</DTPOSTED>
                <TRNAMT>-900.00</TRNAMT>
                <FITID>202603200772</FITID>
                <MEMO>Pix enviado: "Cp :60701190-Raphael Felipe M Brangioni"</MEMO>
                </STMTTRN>
                </BANKTRANLIST>
                </STMTRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inter-transfer.ofx",
                "application/x-ofx",
                ofx.getBytes(StandardCharsets.ISO_8859_1)
        );

        ConciliacaoOfxRelatorioDTO relatorio = conciliacaoOfxService.conciliar(
                2L,
                2,
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                file
        );

        assertThat(relatorio.getTransferenciasConciliadas()).hasSize(1);
        assertThat(relatorio.getTransferenciasConciliadas().get(0).getMovimentacaoId()).isEqualTo(777L);
        assertThat(relatorio.getTransferenciasConciliadas().get(0).getTransferenciaId()).isEqualTo("tx-900");
        assertThat(relatorio.getBancoSemPagamento()).isEmpty();
    }

    @Test
    void conciliarDeveInformarQuandoArquivoJaFoiProcessado() {
        TenantContext.setTenantId("tenantA");
        ContaCorrente contaCorrente = new ContaCorrente(2L, "0001", "10962115", "077", "Inter", BigDecimal.ZERO, 1L, "tenantA", true);
        when(contaCorrenteService.buscarPorId(2L)).thenReturn(Optional.of(contaCorrente));
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(receitaRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(movimentacaoRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(eq(2L), eq("tenantA"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(conciliacaoOfxProcessamentoRepository.findTopByTenantIdAndContaCorrenteIdAndHashArquivoOrderByProcessadoEmDesc(eq("tenantA"), eq(2L), any()))
                .thenReturn(Optional.of(ConciliacaoOfxProcessamento.builder()
                        .processadoEm(LocalDateTime.of(2026, 3, 31, 12, 0))
                        .build()));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "inter.ofx",
                "application/x-ofx",
                """
                OFXHEADER:100
                DATA:OFXSGML
                VERSION:102
                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                <STMTRS>
                <BANKACCTFROM>
                <BANKID>077</BANKID>
                <ACCTID>10962115</ACCTID>
                </BANKACCTFROM>
                <BANKTRANLIST>
                <DTSTART>20260301</DTSTART>
                <DTEND>20260330</DTEND>
                </BANKTRANLIST>
                </STMTRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """.getBytes(StandardCharsets.ISO_8859_1)
        );

        ConciliacaoOfxRelatorioDTO relatorio = conciliacaoOfxService.conciliar(
                2L,
                2,
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                file
        );

        assertThat(relatorio.getArquivoJaProcessado()).isTrue();
        assertThat(relatorio.getUltimoProcessamentoEm()).isEqualTo(LocalDateTime.of(2026, 3, 31, 12, 0));
    }
}
