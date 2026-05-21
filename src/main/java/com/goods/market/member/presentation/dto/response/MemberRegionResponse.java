package com.goods.market.member.presentation.dto.response;

import com.goods.market.member.application.dto.MemberRegionDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

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
    public static MemberRegionResponse from(MemberRegionDto dto) {
        return new MemberRegionResponse(
                dto.memberId(),
                dto.regionId(),
                dto.verifiedAt(),
                dto.primary(),
                dto.dongnm(),
                dto.lat(),
                dto.lng()
        );
    }
}
