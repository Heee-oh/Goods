package com.goods.market.review.presentation.dto.response;

import com.goods.market.review.application.dto.ReviewPromptDto;

public record ReviewPromptResponse(
        Long tradeId,
        String listingTitle,
        String partnerNickname,
        boolean writerIsSeller
) {
    public static ReviewPromptResponse from(ReviewPromptDto dto) {
        return new ReviewPromptResponse(
                dto.tradeId(),
                dto.listingTitle(),
                dto.partnerNickname(),
                dto.writerIsSeller()
        );
    }
}
