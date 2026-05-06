package com.goods.market.review.application;

import com.goods.market.review.domain.Review;
import java.util.Optional;

public interface ReviewQueryService {

    Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);
}
