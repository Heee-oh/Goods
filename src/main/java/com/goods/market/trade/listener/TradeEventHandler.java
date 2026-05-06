package com.goods.market.trade.listener;

import com.goods.market.common.event.events.ListingSoldOutEvent;
import com.goods.market.trade.application.TradeCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeEventHandler {

    private final TradeCommandService tradeCommandService;

    @EventListener
    public void handle(ListingSoldOutEvent event) {
        tradeCommandService.completeFromListing(event.listingId(), event.sellerId(), event.buyerId());
    }
}
