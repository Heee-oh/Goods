package com.goods.market.member.application.dto;

public record MemberUpdateCommand(
        String nickname,
        String profileImage
) {
}
