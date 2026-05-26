package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BatchStatusConverter extends AbstractStringEnumConverter<BatchStatus> {

    public BatchStatusConverter() {
        super(BatchStatus.class);
    }
}
