package com.goods.market.trade.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    SELL("sell"),
    TRADE("trade"),
    FREE("free");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionType from(String value) {
        if (value == null) {
            return null;
        }

        for (TransactionType transactionType : values()) {
            if (transactionType.value.equalsIgnoreCase(value) || transactionType.name().equalsIgnoreCase(value)) {
                return transactionType;
            }
        }

        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }

    public boolean isSell() {
        return this == SELL;
    }

    public boolean isTrade() {
        return this == TRADE;
    }

    public boolean isFree() {
        return this == FREE;
    }
}
