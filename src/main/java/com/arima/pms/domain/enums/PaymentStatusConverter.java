package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentStatusConverter extends AbstractStringEnumConverter<PaymentStatus> {

    public PaymentStatusConverter() {
        super(PaymentStatus.class);
    }
}
