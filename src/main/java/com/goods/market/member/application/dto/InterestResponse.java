package com.goods.market.member.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.querydsl.core.annotations.QueryProjection;

public record InterestResponse(
        Long id,
        Long listingId

) {
    @QueryProjection
    public InterestResponse {
    }
}
