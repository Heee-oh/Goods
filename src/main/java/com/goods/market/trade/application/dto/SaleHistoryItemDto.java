package com.goods.market.trade.application.dto;

import com.goods.market.listing.domain.TransactionType;
import com.goods.market.trade.domain.Price;
import com.querydsl.core.annotations.QueryProjection;

import java.time.Instant;

public record SaleHistoryItemDto(
        long listingId,
        String listingImageUrl,
        String title,
        Price price,
        long buyerId,
        String buyerNickname,
        Instant tradedAt
) {

    @QueryProjection
    public SaleHistoryItemDto {
    }
}
