package com.goods.market.common.auth.presentation.dto.response;

import com.goods.market.common.auth.application.dto.AuthTokenDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record AuthTokenResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long memberId,
        String accessToken,
        long expiresIn
) {
    public static AuthTokenResponse from(AuthTokenDto dto) {
        return new AuthTokenResponse(dto.memberId(), dto.accessToken(), dto.expiresIn());
    }
}
