package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class CalidadFrutaListConverter implements AttributeConverter<List<CalidadFruta>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(List<CalidadFruta> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(Enum::name).collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public List<CalidadFruta> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(SEPARATOR))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(CalidadFruta::valueOf)
                .toList();
    }
}
