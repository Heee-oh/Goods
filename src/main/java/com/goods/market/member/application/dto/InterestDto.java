package com.goods.market.member.application.dto;

import com.querydsl.core.annotations.QueryProjection;

public record InterestDto(
        Long id,
        Long listingId
) {
    @QueryProjection
    public InterestDto {
    }
}
