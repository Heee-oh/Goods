package com.goods.market.review.domain;

public interface ReviewRepository {
    Review save(Review review);

    boolean existsByTradeIdAndWriterId(Long tradeId, Long writerId);
}
