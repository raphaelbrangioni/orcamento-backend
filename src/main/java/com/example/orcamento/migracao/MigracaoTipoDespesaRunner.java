package com.example.orcamento.migracao;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.DespesaParcelada;
import com.example.orcamento.model.Limite;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.DespesaParceladaRepository;
import com.example.orcamento.repository.LimiteRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
public class MigracaoTipoDespesaRunner implements CommandLineRunner {

    private final DespesaRepository despesaRepository;
    private final DespesaParceladaRepository despesaParceladaRepository;
    private final LimiteRepository limiteRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;

    public MigracaoTipoDespesaRunner(
            DespesaRepository despesaRepository,
            DespesaParceladaRepository despesaParceladaRepository,
            LimiteRepository limiteRepository,
            SubcategoriaDespesaRepository subcategoriaDespesaRepository) {
        this.despesaRepository = despesaRepository;
        this.despesaParceladaRepository = despesaParceladaRepository;
        this.limiteRepository = limiteRepository;
        this.subcategoriaDespesaRepository = subcategoriaDespesaRepository;
    }

    // Mapeamento: idAntigo -> idNovaSubcategoria
    private static final Map<Long, Long> MAPA_ID_ANTIGO_NOVO = new HashMap<>();
    static {
        MAPA_ID_ANTIGO_NOVO.put(1L, 109L);   // Internet/TV
        MAPA_ID_ANTIGO_NOVO.put(13L, 1606L); // Veículo
        MAPA_ID_ANTIGO_NOVO.put(14L, 709L);  // Gastos com a Casa
        MAPA_ID_ANTIGO_NOVO.put(15L, 903L);  // Empréstimo Bancário
        MAPA_ID_ANTIGO_NOVO.put(16L, 710L);  // Compra Terreno
        MAPA_ID_ANTIGO_NOVO.put(17L, 2001L); // Seguros
        MAPA_ID_ANTIGO_NOVO.put(18L, 705L);  // Financiamento Imobiliário - Caixa
        MAPA_ID_ANTIGO_NOVO.put(19L, 1801L); // Cartão de Crédito
        MAPA_ID_ANTIGO_NOVO.put(20L, 1202L); // Criptomoeda
        MAPA_ID_ANTIGO_NOVO.put(21L, 1801L); // Parcelamento de Fatura
        MAPA_ID_ANTIGO_NOVO.put(22L, 1406L); // IOF PARCELAMENTO
        MAPA_ID_ANTIGO_NOVO.put(23L, 401L);  // Gasto com Cachorros
        MAPA_ID_ANTIGO_NOVO.put(24L, 308L);  // Compra para Terceiros
        MAPA_ID_ANTIGO_NOVO.put(25L, 311L);  // Vestuário
        MAPA_ID_ANTIGO_NOVO.put(26L, 1007L); // Lazer para Casa
        MAPA_ID_ANTIGO_NOVO.put(27L, 201L);  // Bebidas/Vinhos
        MAPA_ID_ANTIGO_NOVO.put(28L, 601L);  // Item pessoal
        MAPA_ID_ANTIGO_NOVO.put(29L, 1901L); // Dentista
        MAPA_ID_ANTIGO_NOVO.put(30L, 1903L); // Médico
        MAPA_ID_ANTIGO_NOVO.put(31L, 1609L); // Corrida Uber
        MAPA_ID_ANTIGO_NOVO.put(32L, 1608L); // Gasolina
        MAPA_ID_ANTIGO_NOVO.put(33L, 1008L); // Lazer - Passeios
        MAPA_ID_ANTIGO_NOVO.put(34L, 101L);  // Assinatura de streaming
        MAPA_ID_ANTIGO_NOVO.put(35L, 101L);  // Assinatura Google Drive
        MAPA_ID_ANTIGO_NOVO.put(36L, 207L);  // Alimentação fora de casa
        MAPA_ID_ANTIGO_NOVO.put(37L, 602L);  // Barbearia/Salão de beleza
        MAPA_ID_ANTIGO_NOVO.put(38L, 1101L); // Assinatura app academia
        MAPA_ID_ANTIGO_NOVO.put(39L, 1302L); // Mercado
        MAPA_ID_ANTIGO_NOVO.put(40L, 2203L); // Lazer - Viagens
        MAPA_ID_ANTIGO_NOVO.put(41L, 1904L); // Suplementos
        MAPA_ID_ANTIGO_NOVO.put(42L, 103L);  // Consórcio
        MAPA_ID_ANTIGO_NOVO.put(43L, 1703L); // Outros
        MAPA_ID_ANTIGO_NOVO.put(44L, 102L);  // Plano Celular
        MAPA_ID_ANTIGO_NOVO.put(45L, 501L);  // Ofertas Igreja
        MAPA_ID_ANTIGO_NOVO.put(46L, 203L);  // Pedidos Ifood
        MAPA_ID_ANTIGO_NOVO.put(47L, 1401L); // Taxas/Tarifas bancárias
        MAPA_ID_ANTIGO_NOVO.put(48L, 1205L); // Investimento
        MAPA_ID_ANTIGO_NOVO.put(49L, 1902L); // Farmácia
        MAPA_ID_ANTIGO_NOVO.put(50L, 1406L); // IOF Compra Internacional
        MAPA_ID_ANTIGO_NOVO.put(51L, 101L);  // Assinaturas online
        MAPA_ID_ANTIGO_NOVO.put(52L, 706L);  // Impostos
        MAPA_ID_ANTIGO_NOVO.put(53L, 1005L); // Apostas
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Migrar Despesas
        despesaRepository.findAll().forEach(despesa -> {
            // Adapte aqui se o campo antigo foi removido:
            // if (despesa.getTipo() != null && MAPA_ID_ANTIGO_NOVO.containsKey(despesa.getTipo().getId())) { ... }
            // Agora só migre se houver campo antigo! Caso contrário, ignore.
        });
        // Migrar DespesaParcelada
        despesaParceladaRepository.findAll().forEach(dp -> {
            // Adapte aqui se o campo antigo foi removido:
        });
        // Migrar Limite
        limiteRepository.findAll().forEach(limite -> {
            // Adapte aqui se o campo antigo foi removido:
        });
    }
}
