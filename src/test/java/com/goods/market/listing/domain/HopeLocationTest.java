package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class HopeLocationTest {

    @Test
    @DisplayName("희망 거래 위치는 지역과 좌표 유효성을 검증한다")
    void hopeLocationValidation() {
        Assertions.assertThatThrownBy(() -> new HopeLocation(null, new BigDecimal("33.3"), new BigDecimal("127.3")))
                .isInstanceOf(ListingBadRequestException.class);

        Assertions.assertThatThrownBy(() -> new HopeLocation(123, new BigDecimal("127"), new BigDecimal("33.3")))
                .isInstanceOf(ListingBadRequestException.class);

        Assertions.assertThatThrownBy(() -> new HopeLocation(123, new BigDecimal("-90.0001"), new BigDecimal("181")))
                .isInstanceOf(ListingBadRequestException.class);
    }
}