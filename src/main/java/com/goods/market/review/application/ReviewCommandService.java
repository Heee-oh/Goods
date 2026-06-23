package com.goods.market.review.application;

import com.goods.market.review.domain.Review;
import com.goods.market.review.domain.ReviewCreationPolicy;
import com.goods.market.review.domain.ReviewRepository;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
import com.goods.market.trade.application.TradeQueryService;
import com.goods.market.trade.application.dto.ReviewableTrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final TradeQueryService tradeQueryService;
    private final ReviewRepository reviewRepository;


    public Review create(Long tradeId, Long writerId, int rating, String comment) {
        ReviewableTrade trade = tradeQueryService.getReviewableTrade(tradeId);

        // 도메인 검증
        ReviewCreationPolicy.validate(writerId, trade.completed(), trade.buyerId(), trade.sellerId());

        Long targetId = trade.sellerId();
        if (reviewRepository.existsByTradeIdAndWriterId(tradeId, writerId)) {
            throw new ReviewAlreadyExistsException("리뷰가 이미 존재합니다.");
        }

        return reviewRepository.save(Review.create(
                tradeId,
                trade.listingId(),
                writerId,
                targetId,
                rating,
                comment
        ));
    }

}
