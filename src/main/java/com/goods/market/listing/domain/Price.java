package com.goods.market.listing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class Price {

    @Column(name = "price", nullable = false)
    private Long priceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20, nullable = false)
    private TransactionType transactionType;

    public Price(Long priceAmount, TransactionType transactionType) {
        this.transactionType = transactionType;
        this.priceAmount = transactionType != null && transactionType.isSell() ? priceAmount : 0L;
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    void normalize() {
        if (transactionType == null) {
            transactionType = priceAmount != null && priceAmount == 0L ? TransactionType.FREE : TransactionType.SELL;
        }

        if (transactionType.isSell()) {
            if (priceAmount == null) {
                priceAmount = 0L;
            }
            return;
        }

        priceAmount = 0L;
    }

    @Transient
    public TransactionType resolveTransactionType() {
        if (transactionType != null) {
            return transactionType;
        }

        if (priceAmount != null && priceAmount == 0L) {
            return TransactionType.FREE;
        }

        return TransactionType.SELL;
    }

    public boolean isFree() {
        return resolveTransactionType().isFree();
    }

    public boolean isTrade() {
        return resolveTransactionType().isTrade();
    }

    public boolean isSell() {
        return resolveTransactionType().isSell();
    }
}
