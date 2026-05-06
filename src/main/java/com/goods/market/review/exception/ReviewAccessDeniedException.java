package com.goods.market.review.exception;

public class ReviewAccessDeniedException extends RuntimeException {

    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
