package com.example.orcamento.model.converter;

import com.example.orcamento.model.enums.ReceitaTipo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReceitaTipoConverter implements AttributeConverter<ReceitaTipo, String> {
    @Override
    public String convertToDatabaseColumn(ReceitaTipo attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ReceitaTipo convertToEntityAttribute(String dbData) {
        return ReceitaTipo.from(dbData);
    }
}
