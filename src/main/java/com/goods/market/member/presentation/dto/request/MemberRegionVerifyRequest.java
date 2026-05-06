package com.goods.market.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MemberRegionVerifyRequest(
        @NotNull BigDecimal lat,
        @NotNull BigDecimal lng
) {
}
