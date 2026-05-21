package com.goods.market.review.application.dto;

public record ReviewPromptDto(
        Long tradeId,
        String listingTitle,
        String partnerNickname,
        boolean writerIsSeller
) {
}
