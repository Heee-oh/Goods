package com.goods.market.trade.infrastructure;

import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TradeQueryRepositoryCustom {
    Slice<SaleHistoryItemDto> findCompletedSalesBySellerId(Long sellerId, Long lastTradeId,  Pageable pageable);

    Slice<PurchaseHistoryItemDto> findCompletedPurchasesByBuyerId(Long buyerId, Long lastTradeId, Pageable pageable);
}
