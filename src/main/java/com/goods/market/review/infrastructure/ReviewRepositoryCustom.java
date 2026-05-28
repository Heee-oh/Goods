package com.goods.market.review.infrastructure;

import com.goods.market.review.application.dto.ReviewHistoryItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ReviewRepositoryCustom {


    /**
     * 회원의 리뷰 이력 항목들을 조건에 맞게 조회 (No-Offset 페이징)
     */
    Slice<ReviewHistoryItemDto> findReviewHistoryItems(Long memberId, Long lastReviewId, Pageable pageable);
}
