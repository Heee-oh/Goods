package com.goods.market.review.application;

import com.goods.market.review.application.dto.ReviewHistoryItemDto;
import com.goods.market.review.domain.Review;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface ReviewQueryService {

    Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);
    Slice<ReviewHistoryItemDto> findReviewsHistoryItems(Long memberId, Long lastReviewId);
}
