package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SaleStatusConverter extends AbstractStringEnumConverter<SaleStatus> {

    public SaleStatusConverter() {
        super(SaleStatus.class);
    }
}
