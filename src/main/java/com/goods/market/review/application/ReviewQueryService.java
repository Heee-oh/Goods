package com.goods.market.review.application;

import com.goods.market.review.domain.Review;
import com.goods.market.review.application.dto.ReviewPromptDto;
import java.util.Optional;

public interface ReviewQueryService {

    Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);

    Optional<ReviewPromptDto> getReviewPrompt(Long memberId);
}
