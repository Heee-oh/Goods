package com.goods.market.review.infrastructure;

import com.goods.market.review.domain.Review;
import com.goods.market.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewJpaRepository reviewJpaRepository;

    @Override
    public Review save(Review review) {
        return reviewJpaRepository.save(review);
    }

    @Override
    public boolean existsByTradeIdAndWriterId(Long tradeId, Long writerId) {
        return reviewJpaRepository.existsByTradeIdAndWriterId(tradeId, writerId);
    }
}
