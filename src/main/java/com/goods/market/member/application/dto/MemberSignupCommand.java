package com.goods.market.member.application.dto;

import com.goods.market.member.domain.PhoneNumber;

public record MemberSignupCommand(
        String nickname,
        String phoneNumber
) {
}
