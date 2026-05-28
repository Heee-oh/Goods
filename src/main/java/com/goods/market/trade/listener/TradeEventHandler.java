package com.goods.market.trade.listener;

import com.goods.market.common.event.events.ListingSoldOutEvent;
import com.goods.market.trade.application.TradeCommandService;
import com.goods.market.trade.domain.Price;
import com.goods.market.trade.domain.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeEventHandler {

    private final TradeCommandService tradeCommandService;

    @EventListener
    public void handle(ListingSoldOutEvent event) {
        TransactionType transactionType = TransactionType.valueOf(event.price().resolveTransactionType().name());
        Price price = new Price(event.price().getPriceAmount(), transactionType);
        tradeCommandService.complete(event.listingId(), event.sellerId(), event.buyerId(), price);
    }
}
