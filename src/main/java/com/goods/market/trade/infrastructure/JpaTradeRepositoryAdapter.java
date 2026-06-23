package com.goods.market.trade.infrastructure;

import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaTradeRepositoryAdapter implements TradeRepository {

    private final TradeJpaRepository tradeJpaRepository;
    @Override
    public Optional<Trade> findByListingId(Long listingId) {
        return tradeJpaRepository.findByListingId(listingId);
    }

    @Override
    public Trade save(Trade trade) {
        return tradeJpaRepository.save(trade);
    }
}
