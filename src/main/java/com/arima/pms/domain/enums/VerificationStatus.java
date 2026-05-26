package com.arima.pms.domain.enums;

public enum VerificationStatus implements DatabaseValue {
    PENDING("pending"),
    VERIFIED("verified"),
    REJECTED("rejected"),
    EXPIRED("expired");

    private final String dbValue;

    VerificationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
