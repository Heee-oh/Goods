package com.goods.market.member.application.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.time.Instant;

public record MemberRegionDto(
        Long memberId,
        Integer regionId,
        Instant verifiedAt,
        boolean primary,
        String dongnm,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng
) {
    @QueryProjection
    public MemberRegionDto {
    }
}
