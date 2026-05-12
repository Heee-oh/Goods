package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        if (transactionType == null) {
            throw new ListingBadRequestException("Invalid transaction type");
        }

        this.transactionType = transactionType;
        this.priceAmount = normalizePriceAmount(priceAmount, transactionType);
    }

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

    public Long getPriceAmount() {
        TransactionType resolvedTransactionType = resolveTransactionType();
        if (!resolvedTransactionType.isSell()) {
            return 0L;
        }

        return priceAmount == null ? 0L : priceAmount;
    }

    private Long normalizePriceAmount(Long priceAmount, TransactionType transactionType) {
        if (!transactionType.isSell()) {
            return 0L;
        }
        if (priceAmount == null) {
            throw new ListingBadRequestException("Invalid price");
        }

        return priceAmount;
    }
}
