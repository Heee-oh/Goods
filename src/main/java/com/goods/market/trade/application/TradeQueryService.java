package com.goods.market.trade.application;


import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import org.springframework.data.domain.Slice;

public interface TradeQueryService {

    Slice<SaleHistoryItemDto> getSaleHistory(Long sellerId, Long lastTradeId, int size);

    Slice<PurchaseHistoryItemDto> getPurchaseHistory(Long buyerId, Long lastTradeId, int size);

}
