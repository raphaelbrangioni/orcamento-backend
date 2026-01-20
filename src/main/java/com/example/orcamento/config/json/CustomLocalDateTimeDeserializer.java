package com.example.orcamento.config.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        try {
            // Tenta primeiro o formato completo (padrão ISO)
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            // Se falhar, tenta o formato apenas com data
            try {
                LocalDate localDate = LocalDate.parse(dateString, formatter);
                return localDate.atStartOfDay(); // Converte para LocalDateTime no início do dia
            } catch (DateTimeParseException e2) {
                throw new IOException("Falha ao desserializar a data: " + dateString, e2);
            }
        }
    }
}
