package com.example.orcamento.util;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CalculadoraTransacoes {

    public static void main(String[] args) {
        List<Transaction> transacoes = criarListaTransacoes();
        
        // Calcular o total
        double total = 0.0;
        for (Transaction t : transacoes) {
            try {
                String valorFormatado = t.getValor().replace(",", ".");
                total += Double.parseDouble(valorFormatado);
            } catch (NumberFormatException e) {
                log.error("Erro ao converter valor: {}", t.getValor(), e);
            }
        }
        
        System.out.println("Total calculado: R$ " + String.format("%.2f", total));
        System.out.println("Número de transações: " + transacoes.size());
        
        // Identificar transações com parcela > 01
        System.out.println("\nTransações com parcela > 01:");
        for (Transaction t : transacoes) {
            if (t.getParcela() != null && 
                !t.getParcela().startsWith("01/") && 
                !t.getParcela().startsWith("1/")) {
                System.out.println(t.getEstabelecimento() + " - " + t.getParcela() + " - " + t.getValor());
            }
        }
        
        // Agrupar por estabelecimento e data para verificar duplicidades
        System.out.println("\nAgrupamento por estabelecimento e data:");
        Map<String, List<Transaction>> grupos = new HashMap<>();
        for (Transaction t : transacoes) {
            String chave = t.getEstabelecimento() + "_" + t.getDataCompra();
            if (!grupos.containsKey(chave)) {
                grupos.put(chave, new ArrayList<>());
            }
            grupos.get(chave).add(t);
        }
        
        // Mostrar grupos com múltiplas transações
        for (Map.Entry<String, List<Transaction>> entry : grupos.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("\nGrupo: " + entry.getKey());
                for (Transaction t : entry.getValue()) {
                    System.out.println("  - " + t.getEstabelecimento() + " - " + t.getDataCompra() + 
                                      " - " + t.getParcela() + " - " + t.getValor());
                }
            }
        }
    }
    
    private static List<Transaction> criarListaTransacoes() {
        List<Transaction> transacoes = new ArrayList<>();
        
        // Adicionar todas as transações da lista
        transacoes.add(new Transaction("13/04", "MAREGAN -CT", null, "37,25"));
        transacoes.add(new Transaction("22/04", "RDSAUDE ONLINE", null, "111,88"));
        transacoes.add(new Transaction("28/03", "REDE FARROUPILHA GESTAO", null, "120,00"));
        transacoes.add(new Transaction("09/04", "RENNER", null, "-62,90"));
        transacoes.add(new Transaction("11/04", "WINDSURF", null, "93,75"));
        transacoes.add(new Transaction("13/04", "CASAMARIA -CT", null, "41,97"));
        transacoes.add(new Transaction("22/01", "DECOLAR", "04/12", "604,89"));
        transacoes.add(new Transaction("29/01", "CAModas Ltda. c", "03/03", "58,67"));
        transacoes.add(new Transaction("09/04", "DAFITI*4597185750", "01/05", "77,89"));
        transacoes.add(new Transaction("14/04", "RDSAUDE ONLINE", "01/03", "45,88"));
        transacoes.add(new Transaction("09/10", "ECOMMERCE UN*Unde", "07/09", "49,55"));
        transacoes.add(new Transaction("04/10", "BTC PRAIA", "07/12", "150,02"));
        transacoes.add(new Transaction("30/03", "ITAUSHOP", "01/04", "57,90"));
        transacoes.add(new Transaction("10/04", "RENNER", "01/05", "126,93"));
        transacoes.add(new Transaction("27/11", "SAMSUNG", "05/21", "128,60"));
        transacoes.add(new Transaction("27/01", "EL FIERRO", "03/10", "19,90"));
        transacoes.add(new Transaction("03/04", "RDSAUDE ONLINE", "01/02", "88,46"));
        transacoes.add(new Transaction("27/03", "ITAUSHOP", "01/15", "217,53"));
        transacoes.add(new Transaction("26/01", "Panvelfarmacias", "03/03", "58,34"));
        transacoes.add(new Transaction("02/08", "SAMSUNG NO ITAU", "09/21", "133,40"));
        transacoes.add(new Transaction("10/04", "BRS*SHEINCOM", "01/06", "85,71"));
        transacoes.add(new Transaction("26/11", "UNIDAS LOCADORA SA", "05/10", "131,55"));
        transacoes.add(new Transaction("26/03", "PANVEL FARMACIAS", "01/02", "45,45"));
        transacoes.add(new Transaction("10/04", "MODAMUNDIAL*SHEI", "01/06", "60,85"));
        transacoes.add(new Transaction("01/12", "CASSOL CENTELAR", "05/12", "291,62"));
        transacoes.add(new Transaction("23/04", "REDEMUNDI HIPICA", "01/06", "33,34"));
        transacoes.add(new Transaction("10/10", "ZURICH SEGUROS", "07/12", "204,50"));
        transacoes.add(new Transaction("23/08", "PARC=112REDLAR HIP", "09/12", "191,62"));
        transacoes.add(new Transaction("10/04", "Shein *SHEIN", "01/04", "38,14"));
        transacoes.add(new Transaction("03/11", "BRASTEMP *BRAST", "06/12", "324,99"));
        transacoes.add(new Transaction("15/04", "NAT*Natura Pagamen", "01/02", "45,94"));
        transacoes.add(new Transaction("29/11", "PARC=105 Nestle Br", "05/05", "27,80"));
        transacoes.add(new Transaction("09/04", "VINICIUS DA SILVA", "01/03", "76,60"));
        transacoes.add(new Transaction("31/03", "Panvelfarmacias", "01/02", "101,99"));
        transacoes.add(new Transaction("30/03", "Botoclinic", "01/04", "99,75"));
        transacoes.add(new Transaction("30/12", "ITAUSHOP", "04/04", "35,49"));
        transacoes.add(new Transaction("13/11", "ITAUSHOP", "06/10", "19,39"));
        transacoes.add(new Transaction("11/01", "MERCADOLIVRE*2PROD", "04/05", "162,62"));
        transacoes.add(new Transaction("09/04", "RENNER", "01/05", "67,88"));
        transacoes.add(new Transaction("10/04", "LENOVO COMERCIAL E", "01/12", "300,28"));
        transacoes.add(new Transaction("04/11", "ITAUSHOP", "06/06", "33,16"));
        transacoes.add(new Transaction("10/04", "SHEIN *SHEIN.CO", "01/06", "63,65"));
        
        return transacoes;
    }
}
