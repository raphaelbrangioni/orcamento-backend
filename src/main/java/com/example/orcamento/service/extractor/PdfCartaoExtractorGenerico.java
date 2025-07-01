package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class PdfCartaoExtractorGenerico implements PdfCartaoExtractor {
    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        // Implementação genérica: retorna lista vazia ou lógica padrão
        return Collections.emptyList();
    }
}
