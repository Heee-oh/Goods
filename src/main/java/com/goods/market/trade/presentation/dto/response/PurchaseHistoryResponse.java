package com.goods.market.trade.presentation.dto.response;

import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import java.time.Instant;

public record PurchaseHistoryResponse(
        long tradeId,
        long listingId,
        String listingImageUrl,
        String title,
        long priceAmount,
        String transactionType,
        long partnerId,
        String partnerNickname,
        Instant tradedAt,
        boolean reviewWritten
) {
    public static PurchaseHistoryResponse from(PurchaseHistoryItemDto dto) {
        return new PurchaseHistoryResponse(
                dto.tradeId(),
                dto.listingId(),
                dto.listingImageUrl(),
                dto.title(),
                dto.price().getPriceAmount(),
                dto.price().resolveTransactionType().getValue(),
                dto.sellerId(),
                dto.sellerNickname(),
                dto.tradedAt(),
                dto.reviewWritten()
        );
    }
}
