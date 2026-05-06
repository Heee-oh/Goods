package com.goods.market.common.auth.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record AuthTokenResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long memberId,
        String accessToken,
        long expiresIn
) {
}

