package com.goods.market.trade.application.dto;

import com.querydsl.core.annotations.QueryProjection;

public record ReviewableTrade(
        boolean completed,
        Long listingId,
        Long sellerId,
        Long buyerId
) {

    @QueryProjection
    public ReviewableTrade {
    }
}
