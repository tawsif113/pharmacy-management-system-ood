package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PurchaseOrderStatusConverter extends AbstractStringEnumConverter<PurchaseOrderStatus> {

    public PurchaseOrderStatusConverter() {
        super(PurchaseOrderStatus.class);
    }
}
