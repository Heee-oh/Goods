package com.goods.market.trade.domain;

import com.goods.market.trade.exception.AppointmentBadRequestException;

import java.util.Objects;

public class AppointmentSchedulingPolicy {

    private static final String LISTING_STATUS_PUBLISHED = "PUBLISHED";
    private static final String LISTING_STATUS_RESERVED = "RESERVED";
    private static final String LISTING_STATUS_SOLD_OUT = "SOLD_OUT";

    private AppointmentSchedulingPolicy() {
    }

    public static void validateSchedulableListing(String listingStatus, Long reserverId, Long buyerId) {
        if (LISTING_STATUS_SOLD_OUT.equals(listingStatus)) {
            throw new AppointmentBadRequestException("거래 완료된 게시글입니다.");
        }

        if (LISTING_STATUS_RESERVED.equals(listingStatus) && !Objects.equals(reserverId, buyerId)) {
            throw new AppointmentBadRequestException("다른 사람과 거래중입니다.");
        }

        if (!LISTING_STATUS_PUBLISHED.equals(listingStatus) && !LISTING_STATUS_RESERVED.equals(listingStatus)) {
            throw new AppointmentBadRequestException("약속을 만들 수 없는 게시글입니다.");
        }
    }

    public static boolean isReservedListing(String listingStatus) {
        return LISTING_STATUS_RESERVED.equals(listingStatus);
    }
}
