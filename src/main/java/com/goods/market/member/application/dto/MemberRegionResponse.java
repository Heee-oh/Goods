package com.goods.market.member.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.querydsl.core.annotations.QueryProjection;

import java.time.Instant;

public record MemberRegionResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long memberId,
        Integer regionId,
        Instant verifiedAt,
        boolean isPrimary,
        String dongnm,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng
) {
    @QueryProjection
    public MemberRegionResponse {
    }
}
