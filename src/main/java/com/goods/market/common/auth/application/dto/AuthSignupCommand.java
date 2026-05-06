package com.goods.market.common.auth.application.dto;

public record AuthSignupCommand(
        String phoneNumber,
        String nickname,
        Integer regionId
) {
}

