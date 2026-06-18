package com.goods.market.trade.application;

import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.trade.domain.Price;
import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.infrastructure.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeCommandService {

    private final TradeRepository tradeRepository;
    private final DomainEventPublisher domainEventPublisher;


    public Long complete(Long listingId, Long sellerId, Long buyerId, Price price) {
        return tradeRepository.findByListingId(listingId)
                .map(Trade::getId)
                .orElseGet(() -> completeNew(listingId, sellerId, buyerId, price));
    }

    private Long completeNew(Long listingId, Long sellerId, Long buyerId, Price price) {
        Trade savedTrade = tradeRepository.save(Trade.complete(listingId, sellerId, buyerId, price));
        domainEventPublisher.publish(new TradeCompletedEvent(
                savedTrade.getId(),
                listingId,
                sellerId,
                buyerId,
                savedTrade.getPrice()
        ));
        return savedTrade.getId();
    }
}
