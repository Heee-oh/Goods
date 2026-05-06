package com.goods.market.review.application;

import com.goods.market.review.domain.Review;
import com.goods.market.review.infrastructure.ReviewRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private final ReviewRepository reviewRepository;

    @Override
    public Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId) {
        return reviewRepository.findByTradeIdAndWriterIdAndTargetId(tradeId, writerId, targetId);
    }
}
