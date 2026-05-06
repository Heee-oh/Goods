package com.goods.market.listing.exception;

public class ListingAccessDeniedException extends RuntimeException {
    public ListingAccessDeniedException() {
        super("Only the seller can modify this listing");
    }
}

