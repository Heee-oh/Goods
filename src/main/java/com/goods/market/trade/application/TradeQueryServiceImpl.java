package com.goods.market.trade.application;

import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import com.goods.market.trade.infrastructure.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeQueryServiceImpl implements TradeQueryService {

    private final TradeRepository tradeRepository;

    @Override
    public Slice<SaleHistoryItemDto> getSaleHistory(Long sellerId, Long lastTradeId, int size) {
        long cursor = lastTradeId == null ? Long.MAX_VALUE : lastTradeId;
        return tradeRepository.findCompletedSalesBySellerId(
                sellerId,
                cursor,
                PageRequest.of(0, size)
        );
    }

    @Override
    public Slice<PurchaseHistoryItemDto> getPurchaseHistory(Long buyerId, Long lastTradeId, int size) {
        long cursor = lastTradeId == null ? Long.MAX_VALUE : lastTradeId;
        return tradeRepository.findCompletedPurchasesByBuyerId(
                buyerId,
                cursor,
                PageRequest.of(0, size)
        );
    }
}
