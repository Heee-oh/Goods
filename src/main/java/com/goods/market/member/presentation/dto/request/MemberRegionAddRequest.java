package com.goods.market.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record MemberRegionAddRequest(
        @NotNull Integer regionId
) {
}
