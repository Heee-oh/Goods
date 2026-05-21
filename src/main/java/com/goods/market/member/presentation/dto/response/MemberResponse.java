package com.goods.market.member.presentation.dto.response;

import com.goods.market.member.application.dto.MemberDto;

public record MemberResponse(
        String nickname,
        String profileImage,
        int smileScore
) {
    public static MemberResponse from(MemberDto dto) {
        return new MemberResponse(dto.nickname(), dto.profileImage(), dto.smileScore());
    }
}
