package com.goods.market.trade.application;

import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.ReviewableTrade;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import com.goods.market.trade.infrastructure.TradeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeQueryService {

    private final TradeJpaRepository tradeJpaRepository;

    public ReviewableTrade getReviewableTrade(Long tradeId) {
        return tradeJpaRepository.findReviewableTradeById(tradeId)
                .orElseThrow(() -> new RuntimeException("trade not found"));
    }

    public Slice<SaleHistoryItemDto> getSaleHistory(Long sellerId, Long lastTradeId, int size) {
        long cursor = lastTradeId == null ? Long.MAX_VALUE : lastTradeId;
        return tradeJpaRepository.findCompletedSalesBySellerId(
                sellerId,
                cursor,
                PageRequest.of(0, size)
        );
    }

    public Slice<PurchaseHistoryItemDto> getPurchaseHistory(Long buyerId, Long lastTradeId, int size) {
        long cursor = lastTradeId == null ? Long.MAX_VALUE : lastTradeId;
        return tradeJpaRepository.findCompletedPurchasesByBuyerId(
                buyerId,
                cursor,
                PageRequest.of(0, size)
        );
    }
}
