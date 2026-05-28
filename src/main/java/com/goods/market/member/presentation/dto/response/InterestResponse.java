package com.goods.market.member.presentation.dto.response;

import com.goods.market.member.application.dto.InterestDto;

public record InterestResponse(
        Long id,
        Long listingId
) {
    public static InterestResponse from(InterestDto dto) {
        return new InterestResponse(dto.id(), dto.listingId());
    }
}
