package com.goods.market.review.infrastructure;

import com.goods.market.review.domain.Review;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    boolean existsByTradeIdAndWriterId(Long tradeId, Long writerId);

    Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId);

}
