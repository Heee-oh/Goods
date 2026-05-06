package com.goods.market.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull
        Boolean isSeller,

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 500)
        String comment
) {
}
