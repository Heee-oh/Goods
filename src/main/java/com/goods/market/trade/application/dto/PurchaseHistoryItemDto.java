package com.goods.market.trade.application.dto;

import com.goods.market.trade.domain.Price;
import com.querydsl.core.annotations.QueryProjection;

import java.time.Instant;

public record PurchaseHistoryItemDto(
        long tradeId,
        long listingId,
        String listingImageUrl,
        String title,
        Price price,
        long sellerId,
        String sellerNickname,
        Instant tradedAt,
        boolean reviewWritten
) {

    @QueryProjection
    public PurchaseHistoryItemDto {
    }
}
