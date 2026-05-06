package com.goods.market.member.application.dto;

import com.querydsl.core.annotations.QueryProjection;

public record MemberResponse(String nickname, String ProfileImage, int smileScore) {
    @QueryProjection
    public MemberResponse {
    }
}
