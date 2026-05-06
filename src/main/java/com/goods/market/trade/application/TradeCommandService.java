package com.goods.market.trade.application;

public interface TradeCommandService {

    Long completeFromListing(Long listingId, Long sellerId, Long buyerId);

    Long complete(Long listingId, Long sellerId, Long buyerId, Long price);
}
