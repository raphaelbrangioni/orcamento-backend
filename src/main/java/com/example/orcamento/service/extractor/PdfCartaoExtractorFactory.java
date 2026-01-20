package com.example.orcamento.service.extractor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.orcamento.service.extractor.BradescoAmazonPrimePdfExtractor;
import com.example.orcamento.service.extractor.PdfCartaoExtractorCartao1;
import com.example.orcamento.service.extractor.PdfCartaoExtractorCartao3;
import com.example.orcamento.service.extractor.PdfCartaoExtractorCartao5;
import com.example.orcamento.service.extractor.PdfCartaoExtractorGenerico;
import com.example.orcamento.service.extractor.SantanderPdfExtractor;
import com.example.orcamento.service.extractor.ZaffariCardPdfExtractor;
import com.example.orcamento.service.extractor.GenialPdfExtractor;

public class PdfCartaoExtractorFactory {
    private static final Map<String, PdfCartaoExtractor> extractors = new HashMap<>();

    static {
        extractors.put("bradesco", new BradescoAmazonPrimePdfExtractor());
        extractors.put("itau", new PdfCartaoExtractorCartao1());
        extractors.put("cartao1", new PdfCartaoExtractorCartao1());
        extractors.put("cartao3", new PdfCartaoExtractorCartao3());
        extractors.put("inter", new PdfCartaoExtractorCartao5());
        extractors.put("cartao5", new PdfCartaoExtractorCartao5());
        extractors.put("santander", new SantanderPdfExtractor());
        extractors.put("zaffari", new ZaffariCardPdfExtractor());
        extractors.put("genial", new GenialPdfExtractor());
        // Adicione outros modelos aqui conforme necessário
    }

    public static PdfCartaoExtractor getExtractor(String modeloImportacao) {
        if (modeloImportacao == null) {
            return new PdfCartaoExtractorGenerico();
        }
        return extractors.getOrDefault(modeloImportacao.toLowerCase(Locale.ROOT), new PdfCartaoExtractorGenerico());
    }
}
