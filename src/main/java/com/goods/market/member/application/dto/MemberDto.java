package com.goods.market.member.application.dto;

import com.querydsl.core.annotations.QueryProjection;

public record MemberDto(
        String nickname,
        String profileImage,
        int smileScore
) {
    @QueryProjection
    public MemberDto {
    }
}
