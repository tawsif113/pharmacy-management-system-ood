package com.arima.pms.domain.enums;

public enum BatchStatus implements DatabaseValue {
    AVAILABLE("available"),
    RESERVED("reserved"),
    DEPLETED("depleted"),
    EXPIRED("expired"),
    VOIDED("void");

    private final String dbValue;

    BatchStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
