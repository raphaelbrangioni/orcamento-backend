package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import java.io.InputStream;
import java.util.List;

public interface PdfCartaoExtractor {
    List<Transaction> extrair(InputStream pdfInputStream);
}
