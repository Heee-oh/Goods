package com.goods.market.trade.application;

import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.infrastructure.TradeRepository;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeCommandServiceImpl implements TradeCommandService {

    private final ListingJpaRepository listingJpaRepository;
    private final TradeRepository tradeRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public Long completeFromListing(Long listingId, Long sellerId, Long buyerId) {
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(listingId)
                .orElseThrow(EntityNotFoundException::new);

        Long price = listing.getPrice() == null
                ? 0L
                : (listing.getPrice().resolveTransactionType() == TransactionType.FREE ? 0L : listing.getPrice().getPriceAmount());

        return complete(listingId, sellerId, buyerId, price);
    }

    @Override
    public Long complete(Long listingId, Long sellerId, Long buyerId, Long price) {
        return tradeRepository.findByListingId(listingId)
                .map(Trade::getId)
                .orElseGet(() -> completeNew(listingId, sellerId, buyerId, price));
    }

    private Long completeNew(Long listingId, Long sellerId, Long buyerId, Long price) {
        Trade savedTrade = tradeRepository.save(Trade.complete(listingId, sellerId, buyerId, price));
        domainEventPublisher.publish(new TradeCompletedEvent(
                savedTrade.getId(),
                listingId,
                sellerId,
                buyerId,
                price
        ));
        return savedTrade.getId();
    }
}
