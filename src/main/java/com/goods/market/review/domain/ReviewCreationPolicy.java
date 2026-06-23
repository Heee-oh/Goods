package com.goods.market.review.domain;

import com.goods.market.review.exception.ReviewAccessDeniedException;
import com.goods.market.review.exception.ReviewExchangeNotCompletedException;

public class ReviewCreationPolicy {

    public static void validate(Long writerId, boolean completed, Long buyerId, Long sellerId) {
        if (!completed) {
            throw new ReviewExchangeNotCompletedException("완료된 거래만 리뷰를 작성할 수 있습니다.");
        }

        boolean sellerIsNull = sellerId == null;
        boolean writerIsBuyer = buyerId != null && buyerId.equals(writerId);

        if (sellerIsNull || !writerIsBuyer) {
            throw new ReviewAccessDeniedException("리뷰 작성 권한이 없습니다.");
        }
    }
}
