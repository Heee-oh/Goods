package com.goods.market.review.application;

import com.goods.market.review.domain.Review;
import com.goods.market.review.exception.ReviewAccessDeniedException;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
import com.goods.market.review.exception.ReviewExchangeNotCompletedException;
import com.goods.market.review.exception.ReviewExchangeNotFoundException;
import com.goods.market.review.infrastructure.ReviewRepository;
import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TradeStatus;
import com.goods.market.trade.infrastructure.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final TradeRepository tradeRepository;
    private final ReviewRepository reviewRepository;
    public Review create(Long tradeId, Long writerId, boolean isSeller, int rating, String comment) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ReviewExchangeNotFoundException("거래를 찾을 수 없습니다."));

        if (trade.getStatus() != TradeStatus.COMPLETED) {
            throw new ReviewExchangeNotCompletedException("완료된 거래만 리뷰를 작성할 수 있습니다.");
        }

        boolean writerIsSeller = trade.getSellerId().equals(writerId);
        boolean writerIsBuyer = trade.getBuyerId().equals(writerId);
        if ((isSeller && !writerIsSeller) || (!isSeller && !writerIsBuyer)) {
            throw new ReviewAccessDeniedException("리뷰 작성 권한이 없습니다.");
        }

        Long targetId = isSeller ? trade.getBuyerId() : trade.getSellerId();
        if (reviewRepository.existsByTradeIdAndWriterIdAndTargetId(tradeId, writerId, targetId)) {
            throw new ReviewAlreadyExistsException("리뷰가 이미 존재합니다.");
        }

        return reviewRepository.save(Review.create(
                tradeId,
                trade.getListingId(),
                writerId,
                targetId,
                isSeller,
                rating,
                comment
        ));
    }
}
