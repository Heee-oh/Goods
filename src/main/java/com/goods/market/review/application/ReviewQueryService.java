package com.goods.market.review.application;

import com.goods.market.review.application.dto.ReviewHistoryItemDto;
import com.goods.market.review.domain.Review;
import com.goods.market.review.infrastructure.ReviewJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewJpaRepository reviewJpaRepository;
    public Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId) {
        return reviewJpaRepository.findByTradeIdAndWriterIdAndTargetId(tradeId, writerId, targetId);
    }
    public Slice<ReviewHistoryItemDto> findReviewsHistoryItems(Long memberId, Long lastReviewId) {
        return reviewJpaRepository.findReviewHistoryItems(memberId, lastReviewId, PageRequest.of(0, 20));
    }


}
