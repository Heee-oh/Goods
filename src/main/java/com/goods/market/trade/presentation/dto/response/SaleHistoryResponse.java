package com.goods.market.trade.presentation.dto.response;

import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import java.time.Instant;

public record SaleHistoryResponse(
        long tradeId,
        long listingId,
        String listingImageUrl,
        String title,
        long priceAmount,
        String transactionType,
        long partnerId,
        String partnerNickname,
        Instant tradedAt
) {
    public static SaleHistoryResponse from(SaleHistoryItemDto dto) {
        return new SaleHistoryResponse(
                dto.tradeId(),
                dto.listingId(),
                dto.listingImageUrl(),
                dto.title(),
                dto.price().getPriceAmount(),
                dto.price().resolveTransactionType().getValue(),
                dto.buyerId(),
                dto.buyerNickname(),
                dto.tradedAt()
        );
    }
}
