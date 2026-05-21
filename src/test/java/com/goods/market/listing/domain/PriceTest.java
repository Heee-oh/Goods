package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PriceTest {

    @Test
    void rejectsNullTransactionType() {
        Assertions.assertThatThrownBy(() -> new Price(0L, null))
                .isInstanceOf(ListingBadRequestException.class);
    }

    @Test
    void rejectsNullSellPrice() {
        Assertions.assertThatThrownBy(() -> new Price(null, TransactionType.SELL))
                .isInstanceOf(ListingBadRequestException.class);
    }

    @Test
    void normalizesNonSellPriceToZero() {
        Price tradePrice = new Price(1000L, TransactionType.TRADE);
        Price freePrice = new Price(null, TransactionType.FREE);

        Assertions.assertThat(tradePrice.getPriceAmount()).isZero();
        Assertions.assertThat(freePrice.getPriceAmount()).isZero();
    }
}
