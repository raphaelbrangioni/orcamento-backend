package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class PdfCartaoExtractorService {
    @Autowired
    private PdfCartaoExtractorCartao1 extractor1;
    @Autowired
    private PdfCartaoExtractorCartao3 extractor3;
    @Autowired
    private PdfCartaoExtractorCartao5 extractor5;

    public List<Transaction> extrair(Long cartaoId, InputStream pdfInputStream) {
        if (cartaoId == 1L) return extractor1.extrair(pdfInputStream);
        if (cartaoId == 3L) return extractor3.extrair(pdfInputStream);
        if (cartaoId == 5L) return extractor5.extrair(pdfInputStream);
        // Default: pode lançar exception ou retornar vazio
        return List.of();
    }
}
