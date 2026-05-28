package com.goods.market.trade.application;

import com.goods.market.trade.domain.Price;

public interface TradeCommandService {

    Long complete(Long listingId, Long sellerId, Long buyerId, Price price);
}
