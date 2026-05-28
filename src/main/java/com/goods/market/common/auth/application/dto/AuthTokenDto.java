package com.goods.market.common.auth.application.dto;

public record AuthTokenDto(
        Long memberId,
        String accessToken,
        long expiresIn
) {
}
