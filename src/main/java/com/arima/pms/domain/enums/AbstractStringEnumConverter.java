package com.arima.pms.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

@Converter
public abstract class AbstractStringEnumConverter<E extends Enum<E> & DatabaseValue>
        implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected AbstractStringEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(value -> value.getDbValue().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown value '" + dbData + "' for enum " + enumType.getSimpleName()));
    }
}
