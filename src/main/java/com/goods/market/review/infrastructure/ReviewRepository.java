package com.goods.market.review.infrastructure;

import com.goods.market.review.domain.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);

    Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);
}
