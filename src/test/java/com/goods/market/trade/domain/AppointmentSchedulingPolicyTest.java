package com.goods.market.trade.domain;

import com.goods.market.trade.exception.AppointmentBadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentSchedulingPolicyTest {

    @Test
    void validateAllowsPublishedListing() {
        assertThatCode(() -> AppointmentSchedulingPolicy.validateSchedulableListing("PUBLISHED", null, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAllowsReservedListingForSameBuyer() {
        assertThatCode(() -> AppointmentSchedulingPolicy.validateSchedulableListing("RESERVED", 2L, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRejectsSoldOutListing() {
        assertThatThrownBy(() -> AppointmentSchedulingPolicy.validateSchedulableListing("SOLD_OUT", 2L, 2L))
                .isInstanceOf(AppointmentBadRequestException.class)
                .hasMessageContaining("거래 완료된 게시글입니다.");
    }

    @Test
    void validateRejectsReservedListingForOtherBuyer() {
        assertThatThrownBy(() -> AppointmentSchedulingPolicy.validateSchedulableListing("RESERVED", 3L, 2L))
                .isInstanceOf(AppointmentBadRequestException.class)
                .hasMessageContaining("다른 사람과 거래중입니다.");
    }

    @Test
    void validateRejectsUnsupportedListingStatus() {
        assertThatThrownBy(() -> AppointmentSchedulingPolicy.validateSchedulableListing("DRAFT", null, 2L))
                .isInstanceOf(AppointmentBadRequestException.class)
                .hasMessageContaining("약속을 만들 수 없는 게시글입니다.");
    }
}
