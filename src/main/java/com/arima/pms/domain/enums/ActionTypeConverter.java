package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ActionTypeConverter extends AbstractStringEnumConverter<ActionType> {

    public ActionTypeConverter() {
        super(ActionType.class);
    }
}
