package com.goods.market.member.application.dto;

import com.goods.market.member.domain.Member;

public record AppointmentMemberDto(
        Long memberId,
        String nickname
) {
    public static AppointmentMemberDto from(Member member) {
        return new AppointmentMemberDto(
                member.getId(),
                member.getNickname()
        );
    }
}
