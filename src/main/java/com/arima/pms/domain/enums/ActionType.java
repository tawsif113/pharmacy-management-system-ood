package com.arima.pms.domain.enums;

public enum ActionType implements DatabaseValue {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    CANCEL("CANCEL"),
    VOID("VOID"),
    RECEIPT("RECEIPT"),
    SALE("SALE"),
    ADJUSTMENT("ADJUSTMENT");

    private final String dbValue;

    ActionType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
