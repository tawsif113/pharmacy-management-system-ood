package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StockMovementTypeConverter extends AbstractStringEnumConverter<StockMovementType> {

    public StockMovementTypeConverter() {
        super(StockMovementType.class);
    }
}
