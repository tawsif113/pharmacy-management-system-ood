package com.arima.pms.domain.enums;

import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class VerificationStatusConverter extends AbstractStringEnumConverter<VerificationStatus> {

    public VerificationStatusConverter() {
        super(VerificationStatus.class);
    }
}
