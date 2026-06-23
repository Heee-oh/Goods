package com.goods.market.trade.domain;

import java.util.Optional;

public interface TradeRepository {

    Optional<Trade> findByListingId(Long listingId);
    Trade save(Trade trade);
}
